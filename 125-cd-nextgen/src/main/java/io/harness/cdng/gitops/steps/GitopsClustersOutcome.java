package io.harness.cdng.gitops.steps;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.Value;

@Value
@JsonTypeName("gitopsClustersOutcome")
@RecasterAlias("io.harness.cdng.gitops.steps.GitopsClustersOutcome")
@OwnedBy(GITOPS)
public class GitopsClustersOutcome implements Outcome, ExecutionSweepingOutput {
  @NotNull @Singular List<ClusterData> clustersData;

  public void appendCluster(@NotNull String env, @NotNull String clusterName, List<NGVariable> variables) {
    emptyIfNull(variables).stream().map(
        v -> Variable.builder().key(v.getName()).value(v.getCurrentValue().fetchFinalValue()));
    clustersData.add(ClusterData.builder().env(env).clusterName(clusterName).build());
  }

  @Data
  @Builder
  private static class ClusterData {
    String envGroup;
    String env;
    String clusterName;
    List<Variable> variables;
  }

  @Data
  @Builder
  private static class Variable {
    String key;
    Object value;
  }
}
