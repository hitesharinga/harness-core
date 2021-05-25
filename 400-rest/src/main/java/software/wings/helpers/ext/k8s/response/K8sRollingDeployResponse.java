package software.wings.helpers.ext.k8s.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.helm.HelmChartInfo;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesResourceId;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class K8sRollingDeployResponse implements K8sTaskResponse {
  Integer releaseNumber;
  List<K8sPod> k8sPodList;
  String loadBalancer;
  HelmChartInfo helmChartInfo;
  List<KubernetesResourceId> prunedResourcesIds;
}
