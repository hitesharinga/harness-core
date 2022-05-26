package io.harness.cdng.gitops.steps;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pms.execution.utils.AmbianceUtils.getAccountId;
import static io.harness.pms.execution.utils.AmbianceUtils.getOrgIdentifier;
import static io.harness.pms.execution.utils.AmbianceUtils.getProjectIdentifier;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.cdng.envGroup.services.EnvironmentGroupService;
import io.harness.cdng.gitops.service.ClusterService;
import io.harness.cdng.gitops.steps.ClusterStepParameters.EnvClusterRefs;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.gitops.models.Cluster;
import io.harness.gitops.models.ClusterQuery;
import io.harness.gitops.remote.GitopsResourceClient;
import io.harness.ng.beans.PageResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.executable.SyncExecutableWithRbac;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@Slf4j
public class GitopsClustersStep implements SyncExecutableWithRbac<ClusterStepParameters> {
  private static final String GITOPS_SWEEPING_OUTCOME = "gitops";
  private static final int UNLIMITED_SIZE = 100000;

  @Inject private ClusterService clusterService;
  @Inject private EnvironmentGroupService environmentGroupService;
  @Inject private AccessControlClient accessControlClient;
  @Inject private GitopsResourceClient gitopsResourceClient;
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.GITOPS_CLUSTERS.getName())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Override
  public void validateResources(Ambiance ambiance, ClusterStepParameters stepParameters) {}

  @Override
  public StepResponse executeSyncAfterRbac(Ambiance ambiance, ClusterStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    log.info("Starting execution for GitopsClustersStep [{}]", stepParameters);
    List<Cluster> validatedClusters = validatedClusterRefs(ambiance, stepParameters);
    GitopsClustersOutcome outcome = toOutCome(stepParameters, validatedClusters);
    executionSweepingOutputResolver.consume(ambiance, GITOPS_SWEEPING_OUTCOME, outcome, StepOutcomeGroup.STAGE.name());
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }

  @Override
  public Class<ClusterStepParameters> getStepParametersClass() {
    return null;
  }

  private List<Cluster> validatedClusterRefs(Ambiance ambiance, ClusterStepParameters params) {
    final Collection<EnvClusterRefs> envClusterRefs;
    if (params.isDeployToAllEnvs()) {
      checkArgument(EmptyPredicate.isNotEmpty(params.getEnvGroupRef()),
          "environment group must be provided when deploying to all environments");
      Optional<EnvironmentGroupEntity> egEntity = environmentGroupService.get(getAccountId(ambiance),
          getOrgIdentifier(ambiance), getProjectIdentifier(ambiance), params.getEnvGroupRef(), false);
      List<String> envs = egEntity.map(EnvironmentGroupEntity::getEnvIdentifiers).orElse(new ArrayList<>());
      envClusterRefs = envs.stream()
                           .map(e -> EnvClusterRefs.builder().envRef(e).deployToAll(true).build())
                           .collect(Collectors.toList());
    } else {
      envClusterRefs = params.getEnvClusterRefs();
    }

    if (isEmpty(envClusterRefs)) {
      return new ArrayList<>();
    }

    final Collection<String> clusterRefs = fetchClusterRefs(ambiance, envClusterRefs);

    Map<String, Object> filter = ImmutableMap.of("identifier", ImmutableMap.of("$in", clusterRefs));
    try {
      final ClusterQuery query = ClusterQuery.builder()
                                     .accountId(getAccountId(ambiance))
                                     .orgIdentifier(getOrgIdentifier(ambiance))
                                     .projectIdentifier(getProjectIdentifier(ambiance))
                                     .pageIndex(0)
                                     .pageSize(clusterRefs.size())
                                     .filter(filter)
                                     .build();
      final Response<PageResponse<Cluster>> response = gitopsResourceClient.listClusters(query).execute();
      if (response.isSuccessful() && response.body() != null) {
        return response.body().getContent();
      }
      throw new InvalidRequestException(format("Failed to fetch clusters from gitops. %s",
          response.errorBody() != null ? response.errorBody().string() : ""));
    } catch (IOException e) {
      throw new InvalidRequestException("Failed to fetch clusters from gitops. %s", e);
    }
  }

  private Set<String> fetchClusterRefs(Ambiance ambiance, Collection<EnvClusterRefs> envClusterRefs) {
    Set<String> clusterRefs = new HashSet<>();
    clusterRefs.addAll(envClusterRefs.stream()
                           .filter(ec -> !ec.isDeployToAll())
                           .map(EnvClusterRefs::getClusterRefs)
                           .flatMap(Collection::stream)
                           .collect(Collectors.toSet()));

    final Set<String> envsWithAllClustersAsTarget = envClusterRefs.stream()
                                                        .filter(EnvClusterRefs::isDeployToAll)
                                                        .map(EnvClusterRefs::getEnvRef)
                                                        .collect(Collectors.toSet());

    // Todo: Proper handling for large number of clusters
    clusterRefs.addAll(clusterService
                           .listAcrossEnv(0, UNLIMITED_SIZE, getAccountId(ambiance), getOrgIdentifier(ambiance),
                               getProjectIdentifier(ambiance), envsWithAllClustersAsTarget)
                           .stream()
                           .map(io.harness.cdng.gitops.entity.Cluster::getClusterRef)
                           .collect(Collectors.toSet()));
    return clusterRefs;
  }
  private GitopsClustersOutcome toOutCome(ClusterStepParameters params, List<Cluster> validatedClusters) {
    final GitopsClustersOutcome outcome = new GitopsClustersOutcome(new ArrayList<>());
    final Map<String, Cluster> clusterMap =
        validatedClusters.stream().collect(Collectors.toMap(Cluster::getIdentifier, Function.identity()));
    final Set<String> skippedClusters = new HashSet<>();

    if (isNotEmpty(params.getEnvGroupRef())) {
      // Handle env groups
    } else if (isNotEmpty(params.getEnvClusterRefs())) {
      for (EnvClusterRefs env : params.getEnvClusterRefs()) {
        for (String clusterRef : env.getClusterRefs()) {
          if (clusterMap.containsKey(clusterRef)) {
            outcome.appendCluster(env.getEnvRef(), clusterMap.get(clusterRef).getName(), null);
          } else {
            skippedClusters.add(clusterRef);
          }
        }
      }
    }
    log.info("{} clusters {} were processed", outcome.getClustersData().size(), outcome.getClustersData());
    log.warn("{} clusters {} were skipped as they were not present in gitops", skippedClusters.size(), skippedClusters);
    return outcome;
  }
}
