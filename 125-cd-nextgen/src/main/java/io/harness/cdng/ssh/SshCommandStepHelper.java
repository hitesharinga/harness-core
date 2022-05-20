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
import io.harness.delegate.task.ssh.CopyCommandUnit;
import io.harness.delegate.task.ssh.NgCleanupCommandUnit;
import io.harness.delegate.task.ssh.NgCommandUnit;
import io.harness.delegate.task.ssh.NgInitCommandUnit;
import io.harness.delegate.task.ssh.ScriptCommandUnit;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.steps.shellscript.ShellScriptHelperService;
import io.harness.steps.shellscript.ShellScriptInlineSource;
import io.harness.steps.shellscript.ShellScriptSourceWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
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
      @Nonnull Ambiance ambiance, @Nonnull CommandStepParameters executeCommandStepParameters) {
    InfrastructureOutcome infrastructure = getInfrastructureOutcome(ambiance);
    Boolean onDelegate = getBooleanParameterFieldValue(executeCommandStepParameters.onDelegate);
    SshCommandTaskParametersBuilder<?, ?> builder = SshCommandTaskParameters.builder();
    return builder.accountId(AmbianceUtils.getAccountId(ambiance))
        .executeOnDelegate(onDelegate)
        .executionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
        .environmentVariables(
            shellScriptHelperService.getEnvironmentVariables(executeCommandStepParameters.getEnvironmentVariables()))
        .sshInfraDelegateConfig(sshEntityHelper.getSshInfraDelegateConfig(infrastructure, ambiance))
        .commandUnits(mapCommandUnits(executeCommandStepParameters.getCommandUnits(), onDelegate))
        .host(executeCommandStepParameters.getHost())
        .build();
  }

  private List<NgCommandUnit> mapCommandUnits(List<CommandUnitWrapper> stepCommandUnits, boolean onDelegate) {
    if (isEmpty(stepCommandUnits)) {
      throw new InvalidRequestException("No command units found for configured step");
    }
    List<NgCommandUnit> commandUnits = new ArrayList<>(stepCommandUnits.size() + 2);
    commandUnits.add(NgInitCommandUnit.builder().build());

    List<NgCommandUnit> commandUnitsFromStep =
        stepCommandUnits.stream()
            .map(stepCommandUnit
                -> (stepCommandUnit.isScript()) ? mapScriptCommandUnit(stepCommandUnit.getCommandUnit(), onDelegate)
                                                : mapCopyCommandUnit(stepCommandUnit.getCommandUnit()))
            .collect(Collectors.toList());

    commandUnits.addAll(commandUnitsFromStep);
    commandUnits.add(NgCleanupCommandUnit.builder().build());
    return commandUnits;
  }

  private ScriptCommandUnit mapScriptCommandUnit(StepCommandUnit stepCommandUnit, boolean onDelegate) {
    if (stepCommandUnit == null) {
      throw new InvalidRequestException("Invalid command unit format specified");
    }

    if (!(stepCommandUnit.getSpec() instanceof ScriptCommandUnitSpec)) {
      throw new InvalidRequestException("Invalid script command unit specified");
    }

    ScriptCommandUnitSpec spec = (ScriptCommandUnitSpec) stepCommandUnit.getSpec();
    return ScriptCommandUnit.builder()
        .name(stepCommandUnit.getName())
        .script(getShellScript(spec.getSource()))
        .scriptType(spec.getShell().getScriptType())
        .tailFilePatterns(mapTailFilePatterns(spec.getTailFiles()))
        .workingDirectory(shellScriptHelperService.getWorkingDirectory(
            spec.getWorkingDirectory(), spec.getShell().getScriptType(), onDelegate))
        .build();
  }

  private CopyCommandUnit mapCopyCommandUnit(StepCommandUnit stepCommandUnit) {
    if (stepCommandUnit == null) {
      throw new InvalidRequestException("Invalid command unit format specified");
    }

    if (!(stepCommandUnit.getSpec() instanceof CopyCommandUnitSpec)) {
      throw new InvalidRequestException("Invalid copy command unit specified");
    }

    CopyCommandUnitSpec spec = (CopyCommandUnitSpec) stepCommandUnit.getSpec();
    return CopyCommandUnit.builder()
        .name(stepCommandUnit.getName())
        .sourceType(spec.getSourceType().getFileSourceType())
        .destinationPath(getParameterFieldValue(spec.getDestinationPath()))
        .build();
  }

  private List<TailFilePatternDto> mapTailFilePatterns(@Nonnull List<TailFilePattern> tailFiles) {
    if (isEmpty(tailFiles)) {
      return Collections.emptyList();
    }

    return tailFiles.stream()
        .map(it
            -> TailFilePatternDto.builder()
                   .filePath(getParameterFieldValue(it.getTailFile()))
                   .pattern(getParameterFieldValue(it.getTailPattern()))
                   .build())
        .collect(Collectors.toList());
  }

  private String getShellScript(@Nonnull ShellScriptSourceWrapper shellScriptSourceWrapper) {
    ShellScriptInlineSource shellScriptInlineSource = (ShellScriptInlineSource) shellScriptSourceWrapper.getSpec();
    return (String) shellScriptInlineSource.getScript().fetchFinalValue();
  }
}
