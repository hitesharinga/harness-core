package io.harness.cvng.core.jobs;

import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.change.event.metadata.HarnessCDEventMetaData;
import io.harness.cvng.core.beans.change.event.metadata.HarnessCDEventMetaData.HarnessCDEventMetaDataBuilder;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.cvng.core.types.ChangeSourceType;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.deployment.DeploymentEventDTO;
import io.harness.queue.QueueController;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DeploymentChangeEventConsumer extends AbstractStreamConsumer {
  private static final int MAX_WAIT_TIME_SEC = 10;
  ChangeEventService changeEventService;

  @Inject
  public DeploymentChangeEventConsumer(@Named(EventsFrameworkConstants.CD_DEPLOYMENT_EVENT) Consumer consumer,
      QueueController queueController, ChangeEventService changeEventService) {
    super(MAX_WAIT_TIME_SEC, consumer, queueController);
    this.changeEventService = changeEventService;
  }

  @Override
  protected void processMessage(Message message) {
    DeploymentEventDTO deploymentEventDTO;
    try {
      deploymentEventDTO = DeploymentEventDTO.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      log.error("Exception in unpacking DeploymentInfoDTO for key {}", message.getId(), e);
      throw new IllegalStateException(e);
    }
    registerChangeEvent(deploymentEventDTO);
  }

  private void registerChangeEvent(DeploymentEventDTO deploymentEventDTO) {
    HarnessCDEventMetaDataBuilder harnessCDEventMetaDataBuilder =
        HarnessCDEventMetaData.builder()
            .deploymentEndTime(deploymentEventDTO.getDeploymentEndTime())
            .deploymentStartTime(deploymentEventDTO.getDeploymentStartTime())
            .status(deploymentEventDTO.getDeploymentStatus());
    if (deploymentEventDTO.hasExecutionDetails()) {
      harnessCDEventMetaDataBuilder.stageStepId(deploymentEventDTO.getExecutionDetails().getStageSetupId());
      harnessCDEventMetaDataBuilder.planExecutionId(deploymentEventDTO.getExecutionDetails().getPlanExecutionId());
      harnessCDEventMetaDataBuilder.stageId(deploymentEventDTO.getExecutionDetails().getStageId());
      harnessCDEventMetaDataBuilder.pipelineId(deploymentEventDTO.getExecutionDetails().getPipelineId());
    }
    if (deploymentEventDTO.hasArtifactDetails()) {
      harnessCDEventMetaDataBuilder.artifactType(deploymentEventDTO.getArtifactDetails().getArtifactType());
      harnessCDEventMetaDataBuilder.artifactTag(deploymentEventDTO.getArtifactDetails().getArtifactTag());
    }
    ChangeEventDTO changeEventDTO = ChangeEventDTO.builder()
                                        .accountId(deploymentEventDTO.getAccountId())
                                        .orgIdentifier(deploymentEventDTO.getOrgIdentifier())
                                        .projectIdentifier(deploymentEventDTO.getProjectIdentifier())
                                        .serviceIdentifier(deploymentEventDTO.getServiceIdentifier())
                                        .envIdentifier(deploymentEventDTO.getEnvironmentIdentifier())
                                        .eventTime(deploymentEventDTO.getDeploymentEndTime())
                                        .type(ChangeSourceType.HARNESS_CD)
                                        .changeEventMetaData(harnessCDEventMetaDataBuilder.build())
                                        .build();
    changeEventService.register(changeEventDTO);
  }
}
