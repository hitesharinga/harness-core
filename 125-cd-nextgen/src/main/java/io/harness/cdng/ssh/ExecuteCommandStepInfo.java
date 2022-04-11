/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.ssh;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.shellscript.ShellScriptSourceWrapper;
import io.harness.steps.shellscript.ShellType;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.utils.NGVariablesUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.EXECUTE_COMMAND)
@SimpleVisitorHelper(helperClass = ExecuteCommandStepInfoVisitorHelper.class)
@TypeAlias("executeCommandStepInfo")
@RecasterAlias("io.harness.cdng.ssh.ExecuteCommandStepInfo")
public class ExecuteCommandStepInfo extends ExecuteCommandBaseStepInfo implements CDStepInfo, Visitable {
  List<NGVariable> environmentVariables;

  @Builder(builderMethodName = "infoBuilder")
  public ExecuteCommandStepInfo(ShellType shell, ShellScriptSourceWrapper source, List<TailFilePattern> tailFiles,
                                ParameterField<Boolean> onDelegate, ParameterField<List<TaskSelectorYaml>> delegateSelectors,
                                ParameterField<String> workingDirectory, List<NGVariable> environmentVariables) {
    super(shell, source, tailFiles, onDelegate, delegateSelectors, workingDirectory);
    this.environmentVariables = environmentVariables;
  }

  @Override
  @JsonIgnore
  public StepType getStepType() {
    return ExecuteCommandStep.STEP_TYPE;
  }

  @Override
  @JsonIgnore
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return ExecuteCommandStepParameters.infoBuilder()
        .onDelegate(getOnDelegate())
        .shell(getShell())
        .source(getSource())
        .tailFiles(getTailFiles())
        .delegateSelectors(getDelegateSelectors())
        .workingDirectory(getWorkingDirectory())
        .environmentVariables(NGVariablesUtils.getMapOfVariables(environmentVariables, 0L))
        .build();
  }

  @Override
  public ParameterField<List<TaskSelectorYaml>> fetchDelegateSelectors() {
    return getDelegateSelectors();
  }

}
