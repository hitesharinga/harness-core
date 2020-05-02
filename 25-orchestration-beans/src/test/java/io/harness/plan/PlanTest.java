package io.harness.plan;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.plan.Plan.PlanKeys;
import io.harness.rule.Owner;
import io.harness.state.StateType;
import io.harness.testlib.RealMongo;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PlanTest extends OrchestrationBeansTest {
  private static final String PLAN_ID = generateUuid();
  private static final String DUMMY_NODE_1_ID = generateUuid();
  private static final String DUMMY_NODE_2_ID = generateUuid();
  private static final String DUMMY_NODE_3_ID = generateUuid();

  private static final StateType DUMMY_STATE_TYPE = StateType.builder().type("DUMMY").build();

  @Inject private HPersistence hPersistence;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldTestExecutionPlanSave() {
    String savedPlanId = hPersistence.save(buildDummyPlan());
    assertThat(savedPlanId).isNotNull();
    assertThat(savedPlanId).isEqualTo(PLAN_ID);
    Plan savedPlan = hPersistence.createQuery(Plan.class).filter(PlanKeys.uuid, savedPlanId).get();

    assertThat(savedPlan).isNotNull();
    assertThat(savedPlan.getNodes()).hasSize(3);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFetchNode() {
    Plan plan = buildDummyPlan();
    ExecutionNode node1 = plan.fetchNode(DUMMY_NODE_1_ID);
    assertThat(node1).isNotNull();
    assertThat(node1.getName()).isEqualTo("Dummy Node 1");

    ExecutionNode node2 = plan.fetchNode(DUMMY_NODE_2_ID);
    assertThat(node2).isNotNull();
    assertThat(node2.getName()).isEqualTo("Dummy Node 2");

    assertThatThrownBy(() -> plan.fetchNode(generateUuid())).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFetchStartingNode() {
    Plan plan = buildDummyPlan();
    ExecutionNode startingNode = plan.fetchStartingNode();
    assertThat(startingNode).isNotNull();
    assertThat(startingNode.getName()).isEqualTo("Dummy Node 1");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestIsEmpty() {
    Plan plan = Plan.builder().uuid(generateUuid()).build();
    assertThat(plan.isEmpty()).isEqualTo(true);
  }

  private Plan buildDummyPlan() {
    return Plan.builder()
        .node(ExecutionNode.builder().uuid(DUMMY_NODE_1_ID).name("Dummy Node 1").stateType(DUMMY_STATE_TYPE).build())
        .node(ExecutionNode.builder().uuid(DUMMY_NODE_2_ID).name("Dummy Node 2").stateType(DUMMY_STATE_TYPE).build())
        .node(ExecutionNode.builder().uuid(DUMMY_NODE_3_ID).name("Dummy Node 3").stateType(DUMMY_STATE_TYPE).build())
        .startingNodeId(DUMMY_NODE_1_ID)
        .uuid(PLAN_ID)
        .build();
  }
}