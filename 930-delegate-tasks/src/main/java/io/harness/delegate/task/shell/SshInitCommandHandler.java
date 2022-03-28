/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.shell.SshInitCommandTemplates.EXECLAUNCHERV2_SH_FTL;
import static io.harness.delegate.task.shell.SshInitCommandTemplates.TAILWRAPPERV2_SH_FTL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.CommandExecutionException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.shell.AbstractScriptExecutor;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDP)
@Singleton
public class SshInitCommandHandler {
  public String prepareScript(SshCommandTaskParameters taskParameters, AbstractScriptExecutor executor) {
    CommandExecutionStatus status = runPreInitCommand(taskParameters, executor);

    if (CommandExecutionStatus.SUCCESS != status) {
      throw new CommandExecutionException("Failed to run PreInitCommand");
    }

    final String script = taskParameters.getScript();

    boolean includeTailFunctions = isNotEmpty(taskParameters.getTailFilePatterns())
        || StringUtils.contains(script, "harness_utils_start_tail_log_verification")
        || StringUtils.contains(script, "harness_utils_wait_for_tail_log_verification");

    try {
      String generatedExecLauncherV2Command = generateExecLauncherV2Command(taskParameters, includeTailFunctions);
      StringBuilder preparedCommand = new StringBuilder(generatedExecLauncherV2Command);

      if (isEmpty(taskParameters.getTailFilePatterns())) {
        preparedCommand.append(script);
      } else {
        preparedCommand.append(' ').append(generateTailWrapperV2Command(taskParameters));
      }

      return preparedCommand.toString();

    } catch (IOException | TemplateException e) {
      throw new CommandExecutionException("Failed to prepare script", e);
    }
  }

  private CommandExecutionStatus runPreInitCommand(
      SshCommandTaskParameters taskParameters, AbstractScriptExecutor executor) {
    String cmd = String.format("mkdir -p %s", getExecutionStagingDir(taskParameters));
    return executor.executeCommandString(cmd, true);
  }

  public String getExecutionStagingDir(SshCommandTaskParameters taskParameters) {
    return String.format("/tmp/%s", taskParameters.getExecutionId());
  }

  private String generateExecLauncherV2Command(SshCommandTaskParameters taskParameters, boolean includeTailFunctions)
      throws IOException, TemplateException {
    try (StringWriter stringWriter = new StringWriter()) {
      Map<String, Object> templateParams = ImmutableMap.<String, Object>builder()
                                               .put("executionId", taskParameters.getExecutionId())
                                               .put("executionStagingDir", getExecutionStagingDir(taskParameters))
                                               .put("envVariables", taskParameters.getEnvironmentVariables())
                                               .put("safeEnvVariables", taskParameters.getEnvironmentVariables())
                                               .put("scriptWorkingDirectory", taskParameters.getWorkingDirectory())
                                               .put("includeTailFunctions", includeTailFunctions)
                                               .build();
      SshInitCommandTemplates.getTemplate(EXECLAUNCHERV2_SH_FTL).process(templateParams, stringWriter);
      return stringWriter.toString();
    }
  }

  private String generateTailWrapperV2Command(SshCommandTaskParameters taskParameters)
      throws IOException, TemplateException {
    try (StringWriter stringWriter = new StringWriter()) {
      Map<String, Object> templateParams = ImmutableMap.<String, Object>builder()
                                               .put("tailPatterns", taskParameters.getTailFilePatterns())
                                               .put("commandString", taskParameters.getScript())
                                               .build();
      SshInitCommandTemplates.getTemplate(TAILWRAPPERV2_SH_FTL).process(templateParams, stringWriter);
      return stringWriter.toString();
    }
  }
}
