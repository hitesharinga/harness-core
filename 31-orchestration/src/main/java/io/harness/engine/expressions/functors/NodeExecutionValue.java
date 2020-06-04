package io.harness.engine.expressions.functors;

import static java.util.Arrays.asList;

import io.harness.ambiance.Ambiance;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.expressions.NodeExecutionsCache;
import io.harness.engine.services.OutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.expression.LateBindingValue;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
import lombok.Builder;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NodeExecutionValue implements a LateBindingValue which matches expressions starting from startNodeExecution. If we
 * want to resolve fully qualified expressions, startNodeExecution should be null. OOtherwise, it should be the node
 * execution from where we want to start expression evaluation. It supports step parameters and outcomes in expressions.
 */
@Value
@Builder
public class NodeExecutionValue implements LateBindingValue {
  NodeExecutionsCache nodeExecutionsCache;
  OutcomeService outcomeService;
  ExecutionSweepingOutputService executionSweepingOutputService;
  Ambiance ambiance;
  NodeExecution startNodeExecution;
  Set<NodeExecutionEntityType> entityTypes;

  @Override
  public Object bind() {
    Map<String, Object> map = new HashMap<>();
    addChildren(map, startNodeExecution == null ? null : startNodeExecution.getUuid());
    return NodeExecutionMap.builder()
        .nodeExecutionsCache(nodeExecutionsCache)
        .outcomeService(outcomeService)
        .executionSweepingOutputService(executionSweepingOutputService)
        .ambiance(ambiance)
        .nodeExecution(startNodeExecution)
        .entityTypes(entityTypes)
        .children(map)
        .build();
  }

  private void addChildren(Map<String, Object> map, String nodeExecutionId) {
    List<NodeExecution> children = nodeExecutionsCache.fetchChildren(nodeExecutionId);
    for (NodeExecution child : children) {
      if (canAdd(child)) {
        addToMap(map, child);
        continue;
      }

      addChildren(map, child.getUuid());
    }
  }

  private boolean canAdd(NodeExecution nodeExecution) {
    return !nodeExecution.getNode().isSkipExpressionChain()
        && EmptyPredicate.isNotEmpty(nodeExecution.getNode().getIdentifier());
  }

  private void addToMap(Map<String, Object> map, NodeExecution nodeExecution) {
    String key = nodeExecution.getNode().getIdentifier();
    NodeExecutionValue childValue = NodeExecutionValue.builder()
                                        .nodeExecutionsCache(nodeExecutionsCache)
                                        .outcomeService(outcomeService)
                                        .executionSweepingOutputService(executionSweepingOutputService)
                                        .ambiance(ambiance)
                                        .startNodeExecution(nodeExecution)
                                        .entityTypes(entityTypes)
                                        .build();
    map.compute(key, (k, v) -> {
      if (v == null) {
        return childValue;
      }

      Object boundChild = childValue.bind();
      if (v instanceof List) {
        ((List<Object>) v).add(boundChild);
        return v;
      }

      Object boundV = (v instanceof NodeExecutionValue) ? ((NodeExecutionValue) v).bind() : v;
      return asList(boundV, boundChild);
    });
  }
}
