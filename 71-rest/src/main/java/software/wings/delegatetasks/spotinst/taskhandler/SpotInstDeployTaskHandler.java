package software.wings.delegatetasks.spotinst.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.UP_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstDeployTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.spotinst.model.ElastiGroup;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;

import java.util.List;

@Slf4j
@Singleton
@NoArgsConstructor
public class SpotInstDeployTaskHandler extends SpotInstTaskHandler {
  @Override
  protected SpotInstTaskExecutionResponse executeTaskInternal(SpotInstTaskParameters spotInstTaskParameters,
      SpotInstConfig spotInstConfig, AwsConfig awsConfig) throws Exception {
    if (!(spotInstTaskParameters instanceof SpotInstDeployTaskParameters)) {
      String message =
          format("Parameters of unrecognized class: [%s] found while executing setup step. Workflow execution: [%s]",
              spotInstTaskParameters.getClass().getSimpleName(), spotInstTaskParameters.getWorkflowExecutionId());
      logger.error(message);
      return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }

    String spotInstAccountId = spotInstConfig.getSpotInstAccountId();
    String spotInstToken = String.valueOf(spotInstConfig.getSpotInstToken());
    SpotInstDeployTaskParameters deployTaskParameters = (SpotInstDeployTaskParameters) spotInstTaskParameters;
    ElastiGroup newElastiGroup = deployTaskParameters.getNewElastiGroupWithUpdatedCapacity();
    ElastiGroup oldElastiGroup = deployTaskParameters.getOldElastiGroupWithUpdatedCapacity();
    boolean resizeNewFirst = deployTaskParameters.isResizeNewFirst();
    int steadyStateTimeOut = getTimeOut(deployTaskParameters.getSteadyStateTimeOut());

    // For Rollback, we always upsize oldElastiGroup
    if (resizeNewFirst && !deployTaskParameters.isRollback()) {
      if (newElastiGroup != null) {
        updateElastiGroupAndWait(spotInstToken, spotInstAccountId, newElastiGroup,
            deployTaskParameters.getWorkflowExecutionId(), steadyStateTimeOut, deployTaskParameters,
            UP_SCALE_COMMAND_UNIT, UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
      }
      // If BG, do not update oldElastiGroup
      if (oldElastiGroup != null && !deployTaskParameters.isBlueGreen()) {
        updateElastiGroupAndWait(spotInstToken, spotInstAccountId, oldElastiGroup,
            deployTaskParameters.getWorkflowExecutionId(), steadyStateTimeOut, deployTaskParameters,
            DOWN_SCALE_COMMAND_UNIT, DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
      }
    } else {
      if (oldElastiGroup != null) {
        updateElastiGroupAndWait(spotInstToken, spotInstAccountId, oldElastiGroup,
            deployTaskParameters.getWorkflowExecutionId(), steadyStateTimeOut, deployTaskParameters,
            DOWN_SCALE_COMMAND_UNIT, DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
      }
      if (newElastiGroup != null) {
        updateElastiGroupAndWait(spotInstToken, spotInstAccountId, newElastiGroup,
            deployTaskParameters.getWorkflowExecutionId(), steadyStateTimeOut, deployTaskParameters,
            UP_SCALE_COMMAND_UNIT, UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
      }
    }

    List<Instance> newElastiGroupInstances = newElastiGroup != null
        ? getAllEc2InstancesOfElastiGroup(
              awsConfig, deployTaskParameters.getAwsRegion(), spotInstToken, spotInstAccountId, newElastiGroup.getId())
        : emptyList();

    List<Instance> ec2InstancesForOlderElastiGroup = oldElastiGroup != null
        ? getAllEc2InstancesOfElastiGroup(
              awsConfig, deployTaskParameters.getAwsRegion(), spotInstToken, spotInstAccountId, oldElastiGroup.getId())
        : emptyList();

    return SpotInstTaskExecutionResponse.builder()
        .commandExecutionStatus(SUCCESS)
        .spotInstTaskResponse(SpotInstDeployTaskResponse.builder()
                                  .ec2InstancesAdded(newElastiGroupInstances)
                                  .ec2InstancesExisting(ec2InstancesForOlderElastiGroup)
                                  .build())
        .build();
  }
}