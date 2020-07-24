package software.wings.beans;

import static java.lang.String.format;
import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.utils.Utils;

import java.util.Map;
import java.util.Optional;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AzureVMSSInfrastructureMappingKeys")
public class AzureVMSSInfrastructureMapping extends InfrastructureMapping {
  private String baseVMSSName;
  private String userName;
  private String resourceGroupName;
  private String subscriptionId;
  private String password;
  private String hostConnectionAttrs;
  private VMSSAuthType vmssAuthType;
  private VMSSDeploymentType vmssDeploymentType;

  public AzureVMSSInfrastructureMapping() {
    super(InfrastructureMappingType.AZURE_VMSS_INFRA.name());
  }

  @Override
  public void applyProvisionerVariables(
      Map<String, Object> map, NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    throw new UnsupportedOperationException();
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Utils.normalize(format("(AZURE_VMSS) %s",
        Optional.ofNullable(getComputeProviderName()).orElse(getComputeProviderType().toLowerCase())));
  }

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return hostConnectionAttrs;
  }

  @Builder
  public AzureVMSSInfrastructureMapping(String baseVMSSName, String userName, String resourceGroupName,
      String subscriptionId, String password, String hostConnectionAttrs, VMSSAuthType vmssAuthType,
      VMSSDeploymentType vmssDeploymentType) {
    super(InfrastructureMappingType.AZURE_VMSS_INFRA.name());
    this.baseVMSSName = baseVMSSName;
    this.userName = userName;
    this.resourceGroupName = resourceGroupName;
    this.subscriptionId = subscriptionId;
    this.password = password;
    this.hostConnectionAttrs = hostConnectionAttrs;
    this.vmssAuthType = vmssAuthType;
    this.vmssDeploymentType = vmssDeploymentType;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends InfrastructureMapping.YamlWithComputeProvider {
    private String baseVMSSName;
    private String userName;
    private String resourceGroupName;
    private String subscriptionId;
    private String password;
    private String hostConnectionAttrs;
    private VMSSAuthType vmssAuthType;
    private VMSSDeploymentType vmssDeploymentType;

    public Yaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String computeProviderType, String computeProviderName, Map<String, Object> blueprints,
        String baseVMSSName, String userName, String resourceGroupName, String subscriptionId, String password,
        String hostConnectionAttrs, VMSSAuthType vmssAuthType, VMSSDeploymentType vmssDeploymentType) {
      super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, computeProviderType,
          computeProviderName, blueprints);
      this.baseVMSSName = baseVMSSName;
      this.userName = userName;
      this.resourceGroupName = resourceGroupName;
      this.subscriptionId = subscriptionId;
      this.password = password;
      this.hostConnectionAttrs = hostConnectionAttrs;
      this.vmssAuthType = vmssAuthType;
      this.vmssDeploymentType = vmssDeploymentType;
    }
  }
}