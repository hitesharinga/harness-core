package io.harness.beans.stages;

import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("integrationStageStepParameters")
public class IntegrationStageStepParametersPMS implements StepParameters {
  String identifier;
  String name;
  ParameterField<String> description;
  List<NGVariable> variables;
  String type;
  Infrastructure infrastructure;
  List<DependencyElement> dependencies;
  ParameterField<List<String>> sharedPaths;
  ParameterField<String> skipCondition;
  ParameterField<Boolean> enableCloneRepo;
  BuildStatusUpdateParameter buildStatusUpdateParameter;
  String childNodeID;

  public static IntegrationStageStepParametersPMS getStepParameters(StageElementConfig stageElementConfig,
      String childNodeID, BuildStatusUpdateParameter buildStatusUpdateParameter) {
    if (stageElementConfig == null) {
      return IntegrationStageStepParametersPMS.builder().childNodeID(childNodeID).build();
    }
    IntegrationStageConfig integrationStageConfig = (IntegrationStageConfig) stageElementConfig.getStageType();

    return IntegrationStageStepParametersPMS.builder()
        .identifier(stageElementConfig.getIdentifier())
        .name(stageElementConfig.getName())
        .buildStatusUpdateParameter(buildStatusUpdateParameter)
        .description(stageElementConfig.getDescription())
        .infrastructure(integrationStageConfig.getInfrastructure())
        .dependencies(integrationStageConfig.getServiceDependencies())
        .type(stageElementConfig.getType())
        .skipCondition(integrationStageConfig.getSkipCondition())
        .variables(integrationStageConfig.getVariables())
        .childNodeID(childNodeID)
        .sharedPaths(integrationStageConfig.getSharedPaths())
        .enableCloneRepo(integrationStageConfig.getCloneCodebase())
        .build();
  }
}
