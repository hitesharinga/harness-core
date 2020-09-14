package io.harness.cdng.service.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.visitor.helpers.serviceconfig.KubernetesServiceSpecVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonTypeName("Kubernetes")
@SimpleVisitorHelper(helperClass = KubernetesServiceSpecVisitorHelper.class)
public class KubernetesServiceSpec implements ServiceSpec, Visitable {
  ArtifactListConfig artifacts;
  List<ManifestConfigWrapper> manifests;
  List<ManifestOverrideSets> manifestOverrideSets;
  List<ArtifactOverrideSets> artifactOverrideSets;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public String getType() {
    return ServiceDefinitionType.KUBERNETES.getDisplayName();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("artifacts", artifacts);
    manifests.forEach(manifest -> children.add("manifests", manifest));
    // add override sets if necessary
    return children;
  }
}
