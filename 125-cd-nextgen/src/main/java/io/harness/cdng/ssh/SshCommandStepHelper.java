/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.ssh;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.common.ParameterFieldHelper.getBooleanParameterFieldValue;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.delegate.task.shell.SshCommandTaskParameters;
import io.harness.delegate.task.shell.SshCommandTaskParameters.SshCommandTaskParametersBuilder;
import io.harness.delegate.task.shell.TailFilePatternDto;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.shell.ScriptType;
import io.harness.steps.shellscript.ShellScriptHelperService;
import io.harness.steps.shellscript.ShellScriptInlineSource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

@Singleton
@OwnedBy(CDP)
public class SshCommandStepHelper extends CDStepHelper {
  @Inject private ShellScriptHelperService shellScriptHelperService;
  @Inject private SshEntityHelper sshEntityHelper;

  public SshCommandTaskParameters buildSshCommandTaskParameters(
      @Nonnull Ambiance ambiance, @Nonnull ExecuteCommandStepParameters executeCommandStepParameters) {
    ScriptType scriptType = executeCommandStepParameters.getShell().getScriptType();
    InfrastructureOutcome infrastructure = getInfrastructureOutcome(ambiance);
    Boolean onDelegate = getBooleanParameterFieldValue(executeCommandStepParameters.onDelegate);
    SshCommandTaskParametersBuilder<?, ?> builder = SshCommandTaskParameters.builder();
    return builder.accountId(AmbianceUtils.getAccountId(ambiance))
        .executeOnDelegate(onDelegate)
        .executionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
        .script(getShellScript(executeCommandStepParameters))
        .scriptType(executeCommandStepParameters.getShell().getScriptType())
        .workingDirectory(shellScriptHelperService.getWorkingDirectory(
            executeCommandStepParameters.getWorkingDirectory(), scriptType, onDelegate))
        .tailFilePatterns(mapTailFilePatterns(executeCommandStepParameters))
        .environmentVariables(
            shellScriptHelperService.getEnvironmentVariables(executeCommandStepParameters.getEnvironmentVariables()))
        .sshInfraDelegateConfig(sshEntityHelper.getSshInfraDelegateConfig(infrastructure, ambiance))
        .build();
  }

  private List<TailFilePatternDto> mapTailFilePatterns(@Nonnull ExecuteCommandStepParameters stepParameters) {
    if (isEmpty(stepParameters.getTailFiles())) {
      return Collections.emptyList();
    }

    return stepParameters.getTailFiles()
        .stream()
        .map(it
            -> TailFilePatternDto.builder()
                   .filePath(getParameterFieldValue(it.getTailFile()))
                   .pattern(getParameterFieldValue(it.getTailPattern()))
                   .build())
        .collect(Collectors.toList());
  }

  private String getShellScript(@Nonnull ExecuteCommandStepParameters stepParameters) {
    ShellScriptInlineSource shellScriptInlineSource = (ShellScriptInlineSource) stepParameters.getSource().getSpec();
    return (String) shellScriptInlineSource.getScript().fetchFinalValue();
  }
}
