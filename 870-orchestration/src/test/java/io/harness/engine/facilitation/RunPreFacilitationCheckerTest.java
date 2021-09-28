package io.harness.engine.facilitation;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.ExecutionCheck;
import io.harness.engine.OrchestrationEngine;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.VariableResolverTracker;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.run.ExpressionBlock;
import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.expression.PmsEngineExpressionService;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(PIPELINE)
public class RunPreFacilitationCheckerTest extends OrchestrationTestBase {
  @Mock PmsEngineExpressionService pmsEngineExpressionService;
  @Mock EngineExpressionEvaluator engineExpressionEvaluator;
  @Mock VariableResolverTracker variableResolverTracker;
  @Mock OrchestrationEngine engine;
  @Inject MongoTemplate mongoTemplate;
  @Inject @InjectMocks RunPreFacilitationChecker checker;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void performCheckWhenConditionTrue() {
    String whenCondition = "<+pipeline.name>==\"name\"";
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build())
            .status(Status.QUEUED)
            .mode(ExecutionMode.TASK)
            .node(PlanNodeProto.newBuilder()
                      .setUuid(generateUuid())
                      .setStepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                      .setWhenCondition(whenCondition)
                      .build())
            .startTs(System.currentTimeMillis())
            .build();
    mongoTemplate.save(nodeExecution);

    when(engineExpressionEvaluator.getVariableResolverTracker()).thenReturn(variableResolverTracker);
    when(variableResolverTracker.getUsage()).thenReturn(new HashMap<>());
    when(pmsEngineExpressionService.prepareExpressionEvaluator(nodeExecution.getAmbiance()))
        .thenReturn(engineExpressionEvaluator);
    when(engineExpressionEvaluator.evaluateExpression(whenCondition)).thenReturn(true);
    ExecutionCheck check = checker.performCheck(nodeExecution);
    assertThat(check).isNotNull();
    assertThat(check.isProceed()).isTrue();
    verify(engine, times(0)).processStepResponse(any(), any());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void performCheckWhenConditionFalse() {
    String whenCondition = "<+pipeline.name>==\"name\"";
    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build();
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(ambiance)
            .status(Status.QUEUED)
            .mode(ExecutionMode.TASK)
            .node(PlanNodeProto.newBuilder()
                      .setUuid(generateUuid())
                      .setStepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                      .setWhenCondition(whenCondition)
                      .build())
            .startTs(System.currentTimeMillis())
            .build();
    mongoTemplate.save(nodeExecution);

    when(engineExpressionEvaluator.getVariableResolverTracker()).thenReturn(variableResolverTracker);
    when(variableResolverTracker.getUsage()).thenReturn(new HashMap<>());
    when(pmsEngineExpressionService.prepareExpressionEvaluator(nodeExecution.getAmbiance()))
        .thenReturn(engineExpressionEvaluator);
    when(engineExpressionEvaluator.evaluateExpression(whenCondition)).thenReturn(false);
    ExecutionCheck check = checker.performCheck(nodeExecution);
    assertThat(check).isNotNull();
    assertThat(check.isProceed()).isFalse();
    verify(engine, times(1))
        .processStepResponse(ambiance,
            StepResponseProto.newBuilder()
                .setStatus(Status.SKIPPED)
                .setNodeRunInfo(
                    NodeRunInfo.newBuilder().setWhenCondition(whenCondition).setEvaluatedCondition(false).build())
                .build());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void performCheckWhenConditionException() {
    String whenCondition = "<+pipeline.name>==\"name\"";
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build())
            .status(Status.QUEUED)
            .mode(ExecutionMode.TASK)
            .node(PlanNodeProto.newBuilder()
                      .setUuid(generateUuid())
                      .setStepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                      .setWhenCondition(whenCondition)
                      .build())
            .startTs(System.currentTimeMillis())
            .build();
    mongoTemplate.save(nodeExecution);

    when(engineExpressionEvaluator.getVariableResolverTracker()).thenReturn(variableResolverTracker);
    when(variableResolverTracker.getUsage()).thenReturn(new HashMap<>());
    when(pmsEngineExpressionService.prepareExpressionEvaluator(nodeExecution.getAmbiance()))
        .thenReturn(engineExpressionEvaluator);
    InvalidRequestException testException = new InvalidRequestException("TestException");
    when(engineExpressionEvaluator.evaluateExpression(whenCondition)).thenThrow(testException);
    ExecutionCheck check = checker.performCheck(nodeExecution);
    assertThat(check).isNotNull();
    assertThat(check.isProceed()).isFalse();
    verify(engine, times(1)).handleError(nodeExecution.getAmbiance(), testException);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testGetAllExpressions() {
    Map<String, Map<Object, Integer>> usages = new HashMap<>();
    usages.put("stage.name", ImmutableMap.of("stage1", 1));
    usages.put("pipeline.name", ImmutableMap.of("pipeline", 1));
    usages.put("onPipelineStatus", ImmutableMap.of(Boolean.TRUE, 1));
    usages.put("pipeline.currentStatus", ImmutableMap.of("SUCCESS", 1));

    when(engineExpressionEvaluator.getVariableResolverTracker()).thenReturn(variableResolverTracker);
    when(variableResolverTracker.getUsage()).thenReturn(usages);
    List<ExpressionBlock> allExpressions = checker.getAllExpressions(engineExpressionEvaluator);
    assertThat(allExpressions).isNotNull();
    assertThat(allExpressions.size()).isEqualTo(3);
  }
}