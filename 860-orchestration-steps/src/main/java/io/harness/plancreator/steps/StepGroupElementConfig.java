package io.harness.plancreator.steps;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("stepGroupElementConfig")
public class StepGroupElementConfig {
  @ApiModelProperty(hidden = true) String uuid;
  @NotNull @EntityIdentifier String identifier;
  @EntityName String name;

  List<FailureStrategyConfig> failureStrategies;

  @NotNull List<ExecutionWrapperConfig> steps;
  List<ExecutionWrapperConfig> rollbackSteps;
}
