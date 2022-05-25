package io.harness.steps.matrix;

import io.harness.plancreator.strategy.AxisConfig;
import io.harness.plancreator.strategy.ExcludeConfig;
import io.harness.plancreator.strategy.MatrixConfig;
import io.harness.plancreator.strategy.StrategyConfig;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.contracts.execution.MatrixMetadata;
import io.harness.pms.contracts.execution.StrategyMetadata;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class MatrixConfigService implements StrategyConfigService {
  public List<ChildrenExecutableResponse.Child> fetchChildren(StrategyConfig strategyConfig, String childNodeId) {
    MatrixConfig matrixConfig = strategyConfig.getMatrixConfig();
    List<Map<String, String>> combinations = new ArrayList<>();
    List<List<String>> matrixMetadata = new ArrayList<>();
    fetchCombinations(new LinkedHashMap<>(), matrixConfig.getAxes(), combinations, matrixConfig.getExclude(),
        matrixMetadata, new ArrayList<>());
    List<ChildrenExecutableResponse.Child> children = new ArrayList<>();
    int currentIteration = 0;
    int totalCount = combinations.size();
    for (Map<String, String> combination : combinations) {
      children.add(ChildrenExecutableResponse.Child.newBuilder()
                       .setChildNodeId(childNodeId)
                       .setMatrixMetadata(MatrixMetadata.newBuilder()
                                              .addAllMatrixCombination(matrixMetadata.get(currentIteration))
                                              .putAllMatrixValues(combination)
                                              .build())
                       .setStrategyMetadata(StrategyMetadata.newBuilder()
                                                .setCurrentIteration(currentIteration)
                                                .setTotalIterations(totalCount)
                                                .build())
                       .build());
      currentIteration++;
    }
    return children;
  }

  private void fetchCombinations(Map<String, String> currentCombination, Map<String, AxisConfig> axes,
      List<Map<String, String>> combinations, List<ExcludeConfig> exclude, List<List<String>> matrixMetadata,
      List<String> keys) {
    if (axes.size() == 0) {
      if (!exclude.contains(ExcludeConfig.builder().exclude(currentCombination).build())) {
        combinations.add(new HashMap<>(currentCombination));
        matrixMetadata.add(keys);
      }
      return;
    }
    String key = axes.keySet().iterator().next();
    AxisConfig axisValues = axes.get(key);
    axes.remove(key);
    keys.add(key);
    for (String value : axisValues.getAxisValue().getValue()) {
      currentCombination.put(key, value);
      fetchCombinations(currentCombination, axes, combinations, exclude, matrixMetadata, keys);
      currentCombination.remove(key);
    }
    axes.put(key, axisValues);
  }
}
