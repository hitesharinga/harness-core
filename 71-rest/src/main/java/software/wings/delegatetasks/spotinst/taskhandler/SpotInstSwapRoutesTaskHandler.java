package software.wings.delegatetasks.spotinst.taskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.PROD_ELASTI_GROUP_NAME_SUFFIX;
import static io.harness.spotinst.model.SpotInstConstants.RENAME_NEW_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.RENAME_OLD_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.STAGE_ELASTI_GROUP_NAME_SUFFIX;
import static io.harness.spotinst.model.SpotInstConstants.SWAP_ROUTES_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.UP_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.inject.Singleton;

import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import io.harness.delegate.task.aws.LoadBalancerDetailsForBGDeployment;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import io.harness.spotinst.model.ElastiGroupRenameRequest;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.Optional;

@Slf4j
@Singleton
@NoArgsConstructor
public class SpotInstSwapRoutesTaskHandler extends SpotInstTaskHandler {
  protected SpotInstTaskExecutionResponse executeTaskInternal(SpotInstTaskParameters spotInstTaskParameters,
      SpotInstConfig spotInstConfig, AwsConfig awsConfig) throws Exception {
    if (!(spotInstTaskParameters instanceof SpotInstSwapRoutesTaskParameters)) {
      String message =
          format("Parameters of unrecognized class: [%s] found while executing setup step. Workflow execution: [%s]",
              spotInstTaskParameters.getClass().getSimpleName(), spotInstTaskParameters.getWorkflowExecutionId());
      logger.error(message);
      return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }
    String spotInstAccountId = spotInstConfig.getSpotInstAccountId();
    String spotInstToken = String.valueOf(spotInstConfig.getSpotInstToken());
    SpotInstSwapRoutesTaskParameters swapRoutesParameters = (SpotInstSwapRoutesTaskParameters) spotInstTaskParameters;
    if (swapRoutesParameters.isRollback()) {
      return executeRollback(spotInstAccountId, spotInstToken, awsConfig, swapRoutesParameters,
          swapRoutesParameters.getWorkflowExecutionId());
    } else {
      return executeDeploy(spotInstAccountId, spotInstToken, awsConfig, swapRoutesParameters,
          swapRoutesParameters.getWorkflowExecutionId());
    }
  }

  private SpotInstTaskExecutionResponse executeDeploy(String spotInstAccountId, String spotInstToken,
      AwsConfig awsConfig, SpotInstSwapRoutesTaskParameters swapRoutesParameters, String workflowExecutionId)
      throws Exception {
    ExecutionLogCallback logCallback = getLogCallBack(swapRoutesParameters, SWAP_ROUTES_COMMAND_UNIT);
    String prodElastiGroupName =
        format("%s__%s", swapRoutesParameters.getElastiGroupNamePrefix(), PROD_ELASTI_GROUP_NAME_SUFFIX);
    String stageElastiGroupName =
        format("%s__%s", swapRoutesParameters.getElastiGroupNamePrefix(), STAGE_ELASTI_GROUP_NAME_SUFFIX);
    ElastiGroup newElastiGroup = swapRoutesParameters.getNewElastiGroup();
    String newElastiGroupId = (newElastiGroup != null) ? newElastiGroup.getId() : EMPTY;
    ElastiGroup oldElastiGroup = swapRoutesParameters.getOldElastiGroup();
    String oldElastiGroupId = (oldElastiGroup != null) ? oldElastiGroup.getId() : EMPTY;

    if (isNotEmpty(newElastiGroupId)) {
      logCallback.saveExecutionLog(format(
          "Sending request to rename Elasti Group with Id: [%s] to [%s]", newElastiGroupId, prodElastiGroupName));
      spotInstHelperServiceDelegate.updateElastiGroup(spotInstToken, spotInstAccountId, newElastiGroupId,
          ElastiGroupRenameRequest.builder().name(prodElastiGroupName).build());
    }
    if (isNotEmpty(oldElastiGroupId)) {
      logCallback.saveExecutionLog(
          format("Sending request to rename Elasti Group with Id: [%s] to [%s]", oldElastiGroup, stageElastiGroupName));
      spotInstHelperServiceDelegate.updateElastiGroup(spotInstToken, spotInstAccountId, oldElastiGroupId,
          ElastiGroupRenameRequest.builder().name(stageElastiGroupName).build());
    }

    logCallback.saveExecutionLog("Updating Listener Rules for Load Balancer");
    awsElbHelperServiceDelegate.updateListenersForBGDeployment(awsConfig, emptyList(),
        swapRoutesParameters.getLBdetailsForBGDeploymentList(), swapRoutesParameters.getAwsRegion());
    logCallback.saveExecutionLog("Route Updated Successfully", INFO, SUCCESS);

    // Downsize if configured on state
    if (swapRoutesParameters.isDownsizeOldElastiGroup() && isNotEmpty(oldElastiGroupId)) {
      ElastiGroup temp = ElastiGroup.builder()
                             .id(oldElastiGroupId)
                             .name(stageElastiGroupName)
                             .capacity(ElastiGroupCapacity.builder().minimum(0).maximum(0).target(0).build())
                             .build();
      int steadyStateTimeOut = getTimeOut(swapRoutesParameters.getSteadyStateTimeOut());
      updateElastiGroupAndWait(spotInstToken, spotInstAccountId, temp, workflowExecutionId, steadyStateTimeOut,
          swapRoutesParameters, DOWN_SCALE_COMMAND_UNIT, DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
    } else {
      logCallback = getLogCallBack(swapRoutesParameters, DOWN_SCALE_COMMAND_UNIT);
      logCallback.saveExecutionLog("Nothing to Downsize.", INFO, SUCCESS);
      logCallback = getLogCallBack(swapRoutesParameters, DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
      logCallback.saveExecutionLog("No Downsize was required, Swap Route Successfully Completed", INFO, SUCCESS);
    }
    return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(SUCCESS).build();
  }

  private SpotInstTaskExecutionResponse executeRollback(String spotInstAccountId, String spotInstToken,
      AwsConfig awsConfig, SpotInstSwapRoutesTaskParameters swapRoutesParameters, String workflowExecutionId)
      throws Exception {
    String prodElastiGroupName =
        format("%s__%s", swapRoutesParameters.getElastiGroupNamePrefix(), PROD_ELASTI_GROUP_NAME_SUFFIX);
    String stageElastiGroupName =
        format("%s__%s", swapRoutesParameters.getElastiGroupNamePrefix(), STAGE_ELASTI_GROUP_NAME_SUFFIX);
    ElastiGroup newElastiGroup = swapRoutesParameters.getNewElastiGroup();
    String newElastiGroupId = (newElastiGroup != null) ? newElastiGroup.getId() : EMPTY;
    ElastiGroup oldElastiGroup = swapRoutesParameters.getOldElastiGroup();
    String oldElastiGroupId = (oldElastiGroup != null) ? oldElastiGroup.getId() : EMPTY;
    int steadyStateTimeOut = getTimeOut(swapRoutesParameters.getSteadyStateTimeOut());
    ExecutionLogCallback logCallback;

    if (isNotEmpty(oldElastiGroupId)) {
      ElastiGroup temp = ElastiGroup.builder()
                             .id(oldElastiGroupId)
                             .name(prodElastiGroupName)
                             .capacity(oldElastiGroup.getCapacity())
                             .build();
      updateElastiGroupAndWait(spotInstToken, spotInstAccountId, temp, workflowExecutionId, steadyStateTimeOut,
          swapRoutesParameters, UP_SCALE_COMMAND_UNIT, UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);

      logCallback = getLogCallBack(swapRoutesParameters, RENAME_OLD_COMMAND_UNIT);
      logCallback.saveExecutionLog(
          format("Renaming old Elastigroup with id: [%s] to name: [%s]", oldElastiGroupId, prodElastiGroupName));
      spotInstHelperServiceDelegate.updateElastiGroup(spotInstToken, spotInstAccountId, oldElastiGroupId,
          ElastiGroupRenameRequest.builder().name(prodElastiGroupName).build());
      logCallback.saveExecutionLog("Successfully renamed old elastiGroup", INFO, SUCCESS);
    } else {
      createAndFinishEmptyExecutionLog(
          swapRoutesParameters, UP_SCALE_COMMAND_UNIT, "No old elastigroup found for upscaling");
      createAndFinishEmptyExecutionLog(
          swapRoutesParameters, UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT, "No old elastigroup found for upscaling");
      createAndFinishEmptyExecutionLog(
          swapRoutesParameters, RENAME_OLD_COMMAND_UNIT, "No old elastigroup found for renaming");
    }

    restoreRoutesToOriginalStateIfChanged(awsConfig, swapRoutesParameters);

    // Downsize new elastiGrup
    ElastiGroup temp = ElastiGroup.builder()
                           .id(newElastiGroupId)
                           .name(stageElastiGroupName)
                           .capacity(ElastiGroupCapacity.builder().minimum(0).maximum(0).target(0).build())
                           .build();
    updateElastiGroupAndWait(spotInstToken, spotInstAccountId, temp, workflowExecutionId, steadyStateTimeOut,
        swapRoutesParameters, DOWN_SCALE_COMMAND_UNIT, DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);

    // Rename new elastiGroup to STAGE
    logCallback = getLogCallBack(swapRoutesParameters, RENAME_NEW_COMMAND_UNIT);
    logCallback.saveExecutionLog(
        format("Renaming Elasti group with id: [%s] to name: [%s]", newElastiGroupId, stageElastiGroupName));
    spotInstHelperServiceDelegate.updateElastiGroup(spotInstToken, spotInstAccountId, newElastiGroupId,
        ElastiGroupRenameRequest.builder().name(stageElastiGroupName).build());
    logCallback.saveExecutionLog("Completed Rename", INFO, SUCCESS);

    return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(SUCCESS).build();
  }

  private void restoreRoutesToOriginalStateIfChanged(
      AwsConfig awsConfig, SpotInstSwapRoutesTaskParameters swapRoutesParameters) {
    ExecutionLogCallback logCallback = getLogCallBack(swapRoutesParameters, SWAP_ROUTES_COMMAND_UNIT);
    for (LoadBalancerDetailsForBGDeployment lbDetail : swapRoutesParameters.getLBdetailsForBGDeploymentList()) {
      DescribeListenersResult result = awsElbHelperServiceDelegate.describeListenerResult(
          awsConfig, emptyList(), lbDetail.getProdListenerArn(), swapRoutesParameters.getAwsRegion());
      Optional<Action> optionalAction =
          result.getListeners()
              .get(0)
              .getDefaultActions()
              .stream()
              .filter(action -> "forward".equalsIgnoreCase(action.getType()) && isNotEmpty(action.getTargetGroupArn()))
              .findFirst();

      if (optionalAction.isPresent()
          && optionalAction.get().getTargetGroupArn().equals(lbDetail.getStageTargetGroupArn())) {
        logCallback.saveExecutionLog(format("Listener: [%s] is forwarding traffic to: [%s]. Swap routes in rollback",
            lbDetail.getProdListenerArn(), lbDetail.getStageTargetGroupArn()));
        awsElbHelperServiceDelegate.updateListenersForEcsBG(awsConfig, emptyList(), lbDetail.getProdListenerArn(),
            lbDetail.getStageListenerArn(), swapRoutesParameters.getAwsRegion());
      }
    }
    logCallback.saveExecutionLog("Prod Elastigroup is UP with correct traffic", INFO, SUCCESS);
  }

  private void createAndFinishEmptyExecutionLog(
      SpotInstSwapRoutesTaskParameters swapRoutesParameters, String upScaleCommandUnit, String message) {
    ExecutionLogCallback logCallback;
    logCallback = getLogCallBack(swapRoutesParameters, upScaleCommandUnit);
    logCallback.saveExecutionLog(message, INFO, SUCCESS);
  }
}