package io.harness.cvng.core.transformer.changeEvent;

import static io.harness.rule.OwnerRule.ABHIJITH;

import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.change.event.metadata.HarnessCDEventMetaData;
import io.harness.cvng.core.entities.changeSource.event.HarnessCDChangeEvent;
import io.harness.rule.Owner;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HarnessCDChangeEventTransformerTest {
  HarnessCDChangeEventTransformer harnessCDChangeEventTransformer;

  BuilderFactory builderFactory;

  @Before
  public void setup() {
    harnessCDChangeEventTransformer = new HarnessCDChangeEventTransformer();
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void getEntity() {
    ChangeEventDTO changeEventDTO = builderFactory.getHarnessCDChangeEventDTOBuilder().build();
    HarnessCDChangeEvent harnessCDChangeEvent = harnessCDChangeEventTransformer.getEntity(changeEventDTO);
    Assertions.assertThat(harnessCDChangeEvent.getAccountId()).isEqualTo(changeEventDTO.getAccountId());
    Assertions.assertThat(harnessCDChangeEvent.getOrgIdentifier()).isEqualTo(changeEventDTO.getOrgIdentifier());
    Assertions.assertThat(harnessCDChangeEvent.getProjectIdentifier()).isEqualTo(changeEventDTO.getProjectIdentifier());
    Assertions.assertThat(harnessCDChangeEvent.getEventTime()).isEqualTo(changeEventDTO.getEventTime());
    Assertions.assertThat(harnessCDChangeEvent.getType()).isEqualTo(changeEventDTO.getType());
    Assertions.assertThat(harnessCDChangeEvent.getDeploymentEndTime())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentEndTime());
    Assertions.assertThat(harnessCDChangeEvent.getDeploymentStartTime())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentStartTime());
    Assertions.assertThat(harnessCDChangeEvent.getStageStepId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStageStepId());
    Assertions.assertThat(harnessCDChangeEvent.getPlanExecutionId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getPlanExecutionId());
    Assertions.assertThat(harnessCDChangeEvent.getStageId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStageId());
    Assertions.assertThat(harnessCDChangeEvent.getPipelineId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getPipelineId());
    Assertions.assertThat(harnessCDChangeEvent.getArtifactTag())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getArtifactTag());
    Assertions.assertThat(harnessCDChangeEvent.getArtifactType())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getArtifactType());
    Assertions.assertThat(harnessCDChangeEvent.getStatus())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStatus());
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void getMetadata() {
    HarnessCDChangeEvent harnessCDChangeEvent = builderFactory.getHarnessCDChangeEventBuilder().build();
    ChangeEventDTO changeEventDTO = harnessCDChangeEventTransformer.getDTO(harnessCDChangeEvent);
    Assertions.assertThat(harnessCDChangeEvent.getAccountId()).isEqualTo(changeEventDTO.getAccountId());
    Assertions.assertThat(harnessCDChangeEvent.getOrgIdentifier()).isEqualTo(changeEventDTO.getOrgIdentifier());
    Assertions.assertThat(harnessCDChangeEvent.getProjectIdentifier()).isEqualTo(changeEventDTO.getProjectIdentifier());
    Assertions.assertThat(harnessCDChangeEvent.getEventTime()).isEqualTo(changeEventDTO.getEventTime());
    Assertions.assertThat(harnessCDChangeEvent.getType()).isEqualTo(changeEventDTO.getType());
    Assertions.assertThat(harnessCDChangeEvent.getDeploymentEndTime())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentEndTime());
    Assertions.assertThat(harnessCDChangeEvent.getDeploymentStartTime())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getDeploymentStartTime());
    Assertions.assertThat(harnessCDChangeEvent.getStageStepId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStageStepId());
    Assertions.assertThat(harnessCDChangeEvent.getPlanExecutionId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getPlanExecutionId());
    Assertions.assertThat(harnessCDChangeEvent.getPipelineId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getPipelineId());
    Assertions.assertThat(harnessCDChangeEvent.getStageId())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStageId());
    Assertions.assertThat(harnessCDChangeEvent.getArtifactType())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getArtifactType());
    Assertions.assertThat(harnessCDChangeEvent.getArtifactTag())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getArtifactTag());
    Assertions.assertThat(harnessCDChangeEvent.getStatus())
        .isEqualTo(((HarnessCDEventMetaData) changeEventDTO.getChangeEventMetaData()).getStatus());
  }
}