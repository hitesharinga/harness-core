package io.harness.pms.plan.creation;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.plan.GraphLayoutInfo;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class PlanCreatorBlobResponseUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testMerge() {
    assertThatCode(() -> PlanCreationBlobResponseUtils.merge(PlanCreationBlobResponse.newBuilder(), null))
        .doesNotThrowAnyException();
    assertThatCode(()
                       -> PlanCreationBlobResponseUtils.merge(
                           PlanCreationBlobResponse.newBuilder(), PlanCreationBlobResponse.newBuilder().build()))
        .doesNotThrowAnyException();

    PlanCreationBlobResponse.Builder builder =
        PlanCreationBlobResponse.newBuilder()
            .putDependencies("id1", YamlFieldBlob.newBuilder().build())
            .putNodes("id2", PlanNodeProto.newBuilder().setUuid("id2").build())
            .putContext("k1", PlanCreationContextValue.newBuilder().setStringValue("v1").build())
            .setGraphLayoutInfo(GraphLayoutInfo.newBuilder()
                                    .putLayoutNodes("id2", GraphLayoutNode.newBuilder().setNodeUUID("id2").build())
                                    .build());
    PlanCreationBlobResponseUtils.merge(builder,
        PlanCreationBlobResponse.newBuilder()
            .putDependencies("id3", YamlFieldBlob.newBuilder().build())
            .putNodes("id1", PlanNodeProto.newBuilder().setUuid("id1").build())
            .putContext("k2", PlanCreationContextValue.newBuilder().setStringValue("v2").build())
            .setStartingNodeId("id3")
            .setGraphLayoutInfo(GraphLayoutInfo.newBuilder()
                                    .setStartingNodeId("id3")
                                    .putLayoutNodes("id1", GraphLayoutNode.newBuilder().setNodeUUID("id1").build())
                                    .build())
            .build());

    PlanCreationBlobResponse blobResponse = builder.build();
    assertThat(blobResponse.getStartingNodeId()).isEqualTo("id3");
    assertThat(blobResponse.getDependenciesMap().keySet()).containsExactly("id3");
    assertThat(blobResponse.getNodesMap().keySet()).containsExactlyInAnyOrder("id1", "id2");
    assertThat(blobResponse.getContextMap().keySet()).containsExactlyInAnyOrder("k1", "k2");

    assertThat(blobResponse.getGraphLayoutInfo().getStartingNodeId()).isEqualTo("id3");
    assertThat(blobResponse.getGraphLayoutInfo().getLayoutNodesMap().keySet()).containsExactlyInAnyOrder("id1", "id2");
  }
}
