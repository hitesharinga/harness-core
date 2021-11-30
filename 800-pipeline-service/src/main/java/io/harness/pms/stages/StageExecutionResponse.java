package io.harness.pms.stages;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
@Schema(name = "StageExecutionResponse",
    description = "This contains info about a Pipeline Stage needed for stage execution.")
public class StageExecutionResponse {
  String stageIdentifier;
  String stageName;
  String message;
  boolean isToBeBlocked;
  List<String> stagesRequired;
}
