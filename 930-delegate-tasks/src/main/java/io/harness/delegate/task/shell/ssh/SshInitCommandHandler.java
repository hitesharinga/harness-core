/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.shell.ssh;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.shell.SshInitCommandTemplates.EXECLAUNCHERV2_SH_FTL;
import static io.harness.delegate.task.shell.SshInitCommandTemplates.TAILWRAPPERV2_SH_FTL;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.task.shell.SshSessionConfigMapper;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.shell.CommandTaskParameters;
import io.harness.delegate.task.shell.ShellExecutorFactoryNG;
import io.harness.delegate.task.shell.SshCommandTaskParameters;
import io.harness.delegate.task.shell.SshExecutorFactoryNG;
import io.harness.delegate.task.shell.SshInitCommandTemplates;
import io.harness.delegate.task.ssh.NGCommandUnitType;
import io.harness.delegate.task.ssh.NgCommandUnit;
import io.harness.delegate.task.ssh.ScriptCommandUnit;
import io.harness.delegate.utils.SshUtils;
import io.harness.exception.CommandExecutionException;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.shell.AbstractScriptExecutor;
import io.harness.shell.ScriptProcessExecutor;
import io.harness.shell.ScriptSshExecutor;
import io.harness.shell.ScriptType;
import io.harness.shell.ShellExecutorConfig;
import io.harness.shell.SshSessionConfig;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDP)
@Singleton
public class SshInitCommandHandler implements CommandHandler {
  @Inject private SshExecutorFactoryNG sshExecutorFactoryNG;
  @Inject private ShellExecutorFactoryNG shellExecutorFactory;
  @Inject private SshSessionConfigMapper sshSessionConfigMapper;

  @Override
  public CommandExecutionStatus handle(CommandTaskParameters parameters, NgCommandUnit commandUnit,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    if (!(parameters instanceof SshCommandTaskParameters)) {
      throw new InvalidRequestException("Invalid task parameters submitted for command task.");
    }
    SshCommandTaskParameters sshCommandTaskParameters = (SshCommandTaskParameters) parameters;
    AbstractScriptExecutor executor =
        getExecutor(sshCommandTaskParameters, commandUnit.getName(), commandUnitsProgress, logStreamingTaskClient);
    return initAndGenerateScriptCommand(sshCommandTaskParameters, executor);
  }

  private CommandExecutionStatus initAndGenerateScriptCommand(
      SshCommandTaskParameters taskParameters, AbstractScriptExecutor executor) {
    CommandExecutionStatus status = runPreInitCommand(taskParameters, executor);
    if (!CommandExecutionStatus.SUCCESS.equals(status)) {
      return status;
    }

    for (NgCommandUnit commandUnit : taskParameters.getCommandUnits()) {
      if (NGCommandUnitType.SCRIPT.equals(commandUnit.getCommandUnitType())) {
        generateCommand(taskParameters, (ScriptCommandUnit) commandUnit);
      }
    }

    return CommandExecutionStatus.SUCCESS;
  }

  private ScriptSshExecutor getScriptSshExecutor(SshCommandTaskParameters parameters, String commandUnitName,
      CommandUnitsProgress commandUnitsProgress, ILogStreamingTaskClient logStreamingTaskClient) {
    SshSessionConfig sshSessionConfig =
        SshUtils.generateSshSessionConfig(sshSessionConfigMapper, parameters, commandUnitName, null);
    return sshExecutorFactoryNG.getExecutor(sshSessionConfig, logStreamingTaskClient, commandUnitsProgress);
  }

  private ScriptProcessExecutor getScriptProcessExecutor(SshCommandTaskParameters parameters, String commandUnit,
      CommandUnitsProgress commandUnitsProgress, ILogStreamingTaskClient logStreamingTaskClient) {
    ShellExecutorConfig config = getShellExecutorConfig(parameters, commandUnit);
    return shellExecutorFactory.getExecutor(config, logStreamingTaskClient, commandUnitsProgress);
  }

  private ShellExecutorConfig getShellExecutorConfig(SshCommandTaskParameters taskParameters, String commandUnitName) {
    return ShellExecutorConfig.builder()
        .accountId(taskParameters.getAccountId())
        .executionId(taskParameters.getExecutionId())
        .commandUnitName(commandUnitName)
        .environment(taskParameters.getEnvironmentVariables())
        .scriptType(ScriptType.BASH)
        .build();
  }

  private AbstractScriptExecutor getExecutor(SshCommandTaskParameters parameters, String commandUnitName,
      CommandUnitsProgress commandUnitsProgress, ILogStreamingTaskClient logStreamingTaskClient) {
    return parameters.isExecuteOnDelegate()
        ? getScriptProcessExecutor(parameters, commandUnitName, commandUnitsProgress, logStreamingTaskClient)
        : getScriptSshExecutor(parameters, commandUnitName, commandUnitsProgress, logStreamingTaskClient);
  }

  private void generateCommand(SshCommandTaskParameters taskParameters, ScriptCommandUnit commandUnit) {
    final String script = commandUnit.getScript();

    boolean includeTailFunctions = isNotEmpty(commandUnit.getTailFilePatterns())
        || StringUtils.contains(script, "harness_utils_start_tail_log_verification")
        || StringUtils.contains(script, "harness_utils_wait_for_tail_log_verification");

    try {
      String generatedExecLauncherV2Command =
          generateExecLauncherV2Command(taskParameters, commandUnit, includeTailFunctions);
      StringBuilder command = new StringBuilder(generatedExecLauncherV2Command);

      if (isEmpty(commandUnit.getTailFilePatterns())) {
        command.append(script);
      } else {
        command.append(' ').append(generateTailWrapperV2Command(commandUnit));
      }

      commandUnit.setCommand(command.toString());

    } catch (IOException | TemplateException e) {
      throw new CommandExecutionException("Failed to prepare script", e);
    }
  }

  private CommandExecutionStatus runPreInitCommand(
      SshCommandTaskParameters taskParameters, AbstractScriptExecutor executor) {
    String cmd = String.format("mkdir -p %s", getExecutionStagingDir(taskParameters));
    return executor.executeCommandString(cmd, true);
  }

  private String generateExecLauncherV2Command(SshCommandTaskParameters taskParameters, ScriptCommandUnit commandUnit,
      boolean includeTailFunctions) throws IOException, TemplateException {
    String workingDir =
            isNotBlank(commandUnit.getWorkingDirectory()) ? "'" + commandUnit.getWorkingDirectory().trim() + "'" : "";
    try (StringWriter stringWriter = new StringWriter()) {
      Map<String, Object> templateParams = ImmutableMap.<String, Object>builder()
                                               .put("executionId", taskParameters.getExecutionId())
                                               .put("executionStagingDir", getExecutionStagingDir(taskParameters))
                                               .put("envVariables", taskParameters.getEnvironmentVariables())
                                               .put("safeEnvVariables", taskParameters.getEnvironmentVariables())
                                               .put("scriptWorkingDirectory", workingDir)
                                               .put("includeTailFunctions", includeTailFunctions)
                                               .build();
      SshInitCommandTemplates.getTemplate(EXECLAUNCHERV2_SH_FTL).process(templateParams, stringWriter);
      return stringWriter.toString();
    }
  }

  private String generateTailWrapperV2Command(ScriptCommandUnit commandUnit) throws IOException, TemplateException {
    try (StringWriter stringWriter = new StringWriter()) {
      Map<String, Object> templateParams = ImmutableMap.<String, Object>builder()
                                               .put("tailPatterns", commandUnit.getTailFilePatterns())
                                               .put("commandString", commandUnit.getScript())
                                               .build();
      SshInitCommandTemplates.getTemplate(TAILWRAPPERV2_SH_FTL).process(templateParams, stringWriter);
      return stringWriter.toString();
    }
  }
}
