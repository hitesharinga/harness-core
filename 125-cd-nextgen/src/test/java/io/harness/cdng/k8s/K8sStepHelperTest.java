package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.k8s.K8sStepHelper.K8S_SUPPORTED_MANIFEST_TYPES;
import static io.harness.cdng.k8s.K8sStepHelper.MISSING_INFRASTRUCTURE_ERROR;
import static io.harness.delegate.beans.connector.ConnectorType.AWS;
import static io.harness.delegate.beans.connector.ConnectorType.GCP;
import static io.harness.delegate.beans.connector.ConnectorType.HTTP_HELM_REPO;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.VIKAS_S;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome.K8sDirectInfrastructureOutcomeBuilder;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.k8s.beans.HelmValuesFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.steps.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.GcsStoreConfig;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.HelmChartManifestOutcome;
import io.harness.cdng.manifest.yaml.HelmCommandFlagType;
import io.harness.cdng.manifest.yaml.HelmManifestCommandFlag;
import io.harness.cdng.manifest.yaml.HttpStoreConfig;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.KustomizeManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.OpenshiftManifestOutcome;
import io.harness.cdng.manifest.yaml.OpenshiftParamManifestOutcome;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthType;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthenticationDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.storeconfig.GcsHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.delegate.task.helm.HelmValuesFetchRequest;
import io.harness.delegate.task.helm.HelmValuesFetchResponse;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.delegate.task.k8s.K8sManifestDelegateConfig;
import io.harness.delegate.task.k8s.KustomizeManifestDelegateConfig;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.delegate.task.k8s.ManifestType;
import io.harness.delegate.task.k8s.OpenshiftManifestDelegateConfig;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.helm.HelmSubCommandType;
import io.harness.k8s.model.HelmVersion;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.pms.data.OrchestrationRefType;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.execution.invokers.StrategyHelper;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(CDP)
public class K8sStepHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ConnectorService connectorService;
  @Mock private GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;
  @Mock private EngineExpressionService engineExpressionService;
  @Mock private OutcomeService outcomeService;
  @Mock private K8sStepExecutor k8sStepExecutor;
  @Mock private KryoSerializer kryoSerializer;
  @InjectMocks private K8sStepHelper k8sStepHelper;

  private final Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", "test-account").build();

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetProjectConnector() {
    Ambiance ambiance = getAmbiance();
    ConnectorInfoDTO connectorDTO = ConnectorInfoDTO.builder().build();
    Optional<ConnectorResponseDTO> connectorDTOOptional =
        Optional.of(ConnectorResponseDTO.builder().connector(connectorDTO).build());
    doReturn(connectorDTOOptional).when(connectorService).get("account1", "org1", "project1", "abcConnector");

    ConnectorInfoDTO actualConnector = k8sStepHelper.getConnector("abcConnector", ambiance);
    assertThat(actualConnector).isEqualTo(connectorDTO);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetOrgConnector() {
    Ambiance ambiance = getAmbiance();
    ConnectorInfoDTO connectorDTO = ConnectorInfoDTO.builder().build();
    Optional<ConnectorResponseDTO> connectorDTOOptional =
        Optional.of(ConnectorResponseDTO.builder().connector(connectorDTO).build());
    doReturn(connectorDTOOptional).when(connectorService).get("account1", "org1", null, "abcConnector");

    ConnectorInfoDTO actualConnector = k8sStepHelper.getConnector("org.abcConnector", ambiance);
    assertThat(actualConnector).isEqualTo(connectorDTO);
  }

  @Test
  @Owner(developers = VIKAS_S)
  @Category(UnitTests.class)
  public void testGetReleaseName() {
    // Invalid formats
    assertThatThrownBy(()
                           -> k8sStepHelper.getReleaseName(
                               K8sDirectInfrastructureOutcome.builder().releaseName("NameWithUpperCase").build()))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(
        ()
            -> k8sStepHelper.getReleaseName(
                K8sGcpInfrastructureOutcome.builder().releaseName("-starting.with.non.alphanumeric").build()))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(
        ()
            -> k8sStepHelper.getReleaseName(
                K8sDirectInfrastructureOutcome.builder().releaseName(".starting.with.non.alphanumeric").build()))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(
        ()
            -> k8sStepHelper.getReleaseName(
                K8sGcpInfrastructureOutcome.builder().releaseName("containing)invalid.characters+").build()))
        .isInstanceOf(InvalidRequestException.class);

    // Valid Formats
    k8sStepHelper.getReleaseName(K8sDirectInfrastructureOutcome.builder().releaseName("alphanumeriname124").build());
    k8sStepHelper.getReleaseName(K8sGcpInfrastructureOutcome.builder().releaseName("1starting.with.number").build());
    k8sStepHelper.getReleaseName(
        K8sDirectInfrastructureOutcome.builder().releaseName("starting.with.alphabet").build());
    k8sStepHelper.getReleaseName(K8sGcpInfrastructureOutcome.builder().releaseName("containing.dot").build());
    k8sStepHelper.getReleaseName(K8sDirectInfrastructureOutcome.builder().releaseName("containing-hyphen").build());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetAccountConnector() {
    Ambiance ambiance = getAmbiance();
    ConnectorInfoDTO connectorDTO = ConnectorInfoDTO.builder().build();
    Optional<ConnectorResponseDTO> connectorDTOOptional =
        Optional.of(ConnectorResponseDTO.builder().connector(connectorDTO).build());

    doReturn(connectorDTOOptional).when(connectorService).get("account1", null, null, "abcConnector");
    doReturn(Optional.empty()).when(connectorService).get("account1", "org1", null, "abcConnector");
    doReturn(Optional.empty()).when(connectorService).get("account1", "org1", "project1", "abcConnector");

    ConnectorInfoDTO actualConnector = k8sStepHelper.getConnector("account.abcConnector", ambiance);
    assertThat(actualConnector).isEqualTo(connectorDTO);

    assertThatThrownBy(() -> k8sStepHelper.getConnector("org.abcConnector", ambiance))
        .hasMessageContaining("Connector not found for identifier : [org.abcConnector]");

    assertThatThrownBy(() -> k8sStepHelper.getConnector("abcConnector", ambiance))
        .hasMessageContaining("Connector not found for identifier : [abcConnector]");
  }

  public Ambiance getAmbiance() {
    Map<String, String> setupAbstractions = new HashMap<>();

    setupAbstractions.put(SetupAbstractionKeys.accountId, "account1");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "org1");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "project1");

    return Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).build();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetK8sManifestsOutcome() {
    assertThatThrownBy(() -> k8sStepHelper.getK8sSupportedManifestOutcome(Collections.emptyList()))
        .hasMessageContaining(
            "Manifests are mandatory for K8s step. Select one from " + String.join(", ", K8S_SUPPORTED_MANIFEST_TYPES));

    K8sManifestOutcome k8sManifestOutcome = K8sManifestOutcome.builder().build();
    ValuesManifestOutcome valuesManifestOutcome = ValuesManifestOutcome.builder().build();
    List<ManifestOutcome> serviceManifestOutcomes = new ArrayList<>();
    serviceManifestOutcomes.add(k8sManifestOutcome);
    serviceManifestOutcomes.add(valuesManifestOutcome);

    ManifestOutcome actualK8sManifest = k8sStepHelper.getK8sSupportedManifestOutcome(serviceManifestOutcomes);
    assertThat(actualK8sManifest).isEqualTo(k8sManifestOutcome);

    K8sManifestOutcome anotherK8sManifest = K8sManifestOutcome.builder().build();
    serviceManifestOutcomes.add(anotherK8sManifest);

    assertThatThrownBy(() -> k8sStepHelper.getK8sSupportedManifestOutcome(serviceManifestOutcomes))
        .hasMessageContaining(
            "There can be only a single manifest. Select one from " + String.join(", ", K8S_SUPPORTED_MANIFEST_TYPES));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetHelmChartManifestsOutcome() {
    HelmChartManifestOutcome helmChartManifestOutcome =
        HelmChartManifestOutcome.builder().helmVersion(HelmVersion.V3).skipResourceVersioning(true).build();
    ValuesManifestOutcome valuesManifestOutcome = ValuesManifestOutcome.builder().build();
    List<ManifestOutcome> manifestOutcomes = new ArrayList<>();
    manifestOutcomes.add(helmChartManifestOutcome);
    manifestOutcomes.add(valuesManifestOutcome);

    assertThat(k8sStepHelper.getK8sSupportedManifestOutcome(manifestOutcomes)).isEqualTo(helmChartManifestOutcome);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetAggregatedValuesManifests() {
    K8sManifestOutcome k8sManifestOutcome = K8sManifestOutcome.builder().build();
    ValuesManifestOutcome valuesManifestOutcome = ValuesManifestOutcome.builder().build();
    List<ManifestOutcome> serviceManifestOutcomes = new ArrayList<>();
    serviceManifestOutcomes.add(k8sManifestOutcome);
    serviceManifestOutcomes.add(valuesManifestOutcome);

    List<ValuesManifestOutcome> aggregatedValuesManifests =
        k8sStepHelper.getAggregatedValuesManifests(serviceManifestOutcomes);
    assertThat(aggregatedValuesManifests).hasSize(1);
    assertThat(aggregatedValuesManifests.get(0)).isEqualTo(valuesManifestOutcome);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetOpenshiftParamManifests() {
    OpenshiftManifestOutcome openshiftManifestOutcome = OpenshiftManifestOutcome.builder().build();
    OpenshiftParamManifestOutcome openshiftParamManifestOutcome = OpenshiftParamManifestOutcome.builder().build();
    List<ManifestOutcome> serviceManifestOutcomes = new ArrayList<>();
    serviceManifestOutcomes.add(openshiftManifestOutcome);
    serviceManifestOutcomes.add(openshiftParamManifestOutcome);

    List<OpenshiftParamManifestOutcome> openshiftParamManifests =
        k8sStepHelper.getOpenshiftParamManifests(serviceManifestOutcomes);
    assertThat(openshiftParamManifests).hasSize(1);
    assertThat(openshiftParamManifests.get(0)).isEqualTo(openshiftParamManifestOutcome);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetManifestDelegateConfigForK8s() {
    K8sManifestOutcome manifestOutcome =
        K8sManifestOutcome.builder()
            .store(GitStore.builder()
                       .branch(ParameterField.createValueField("test"))
                       .connectorRef(ParameterField.createValueField("org.connectorRef"))
                       .paths(ParameterField.createValueField(Arrays.asList("file1", "file2")))
                       .build())
            .build();

    doReturn(
        Optional.of(ConnectorResponseDTO.builder()
                        .connector(ConnectorInfoDTO.builder().connectorConfig(GitConfigDTO.builder().build()).build())
                        .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());

    ManifestDelegateConfig delegateConfig = k8sStepHelper.getManifestDelegateConfig(manifestOutcome, ambiance);
    assertThat(delegateConfig.getManifestType()).isEqualTo(ManifestType.K8S_MANIFEST);
    assertThat(delegateConfig).isInstanceOf(K8sManifestDelegateConfig.class);
    assertThat(delegateConfig.getStoreDelegateConfig()).isNotNull();
    assertThat(delegateConfig.getStoreDelegateConfig()).isInstanceOf(GitStoreDelegateConfig.class);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetManifestDelegateConfigForHelmChart() {
    List<HelmManifestCommandFlag> commandFlags = Arrays.asList(HelmManifestCommandFlag.builder()
                                                                   .commandType(HelmCommandFlagType.Fetch)
                                                                   .flag(ParameterField.createValueField("--test"))
                                                                   .build(),
        HelmManifestCommandFlag.builder()
            .commandType(HelmCommandFlagType.Version)
            .flag(ParameterField.createValueField("--test2"))
            .build());
    HelmChartManifestOutcome manifestOutcome =
        HelmChartManifestOutcome.builder()
            .store(GitStore.builder()
                       .branch(ParameterField.createValueField("test"))
                       .connectorRef(ParameterField.createValueField("org.connectorRef"))
                       .paths(ParameterField.createValueField(Arrays.asList("file1", "file2")))
                       .build())
            .skipResourceVersioning(true)
            .helmVersion(HelmVersion.V3)
            .commandFlags(commandFlags)
            .build();

    doReturn(
        Optional.of(ConnectorResponseDTO.builder()
                        .connector(ConnectorInfoDTO.builder().connectorConfig(GitConfigDTO.builder().build()).build())
                        .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());

    ManifestDelegateConfig delegateConfig = k8sStepHelper.getManifestDelegateConfig(manifestOutcome, ambiance);
    assertThat(delegateConfig.getManifestType()).isEqualTo(ManifestType.HELM_CHART);
    assertThat(delegateConfig).isInstanceOf(HelmChartManifestDelegateConfig.class);
    HelmChartManifestDelegateConfig helmChartDelegateConfig = (HelmChartManifestDelegateConfig) delegateConfig;
    assertThat(helmChartDelegateConfig.getStoreDelegateConfig()).isNotNull();
    assertThat(helmChartDelegateConfig.getStoreDelegateConfig()).isInstanceOf(GitStoreDelegateConfig.class);
    assertThat(helmChartDelegateConfig.getHelmVersion()).isEqualTo(HelmVersion.V3);
    assertThat(helmChartDelegateConfig.getHelmCommandFlag().getValueMap())
        .containsKeys(HelmSubCommandType.FETCH, HelmSubCommandType.VERSION);
    assertThat(helmChartDelegateConfig.getHelmCommandFlag().getValueMap()).containsValues("--test", "--test2");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldReturnSkipResourceVersioning() {
    boolean result =
        k8sStepHelper.getSkipResourceVersioning(K8sManifestOutcome.builder().skipResourceVersioning(true).build());
    assertThat(result).isTrue();
    result =
        k8sStepHelper.getSkipResourceVersioning(K8sManifestOutcome.builder().skipResourceVersioning(false).build());
    assertThat(result).isFalse();
    result = k8sStepHelper.getSkipResourceVersioning(
        HelmChartManifestOutcome.builder().skipResourceVersioning(true).build());
    assertThat(result).isTrue();
    result = k8sStepHelper.getSkipResourceVersioning(
        HelmChartManifestOutcome.builder().skipResourceVersioning(false).build());
    assertThat(result).isFalse();
    result = k8sStepHelper.getSkipResourceVersioning(
        KustomizeManifestOutcome.builder().skipResourceVersioning(true).build());
    assertThat(result).isTrue();
    result = k8sStepHelper.getSkipResourceVersioning(
        KustomizeManifestOutcome.builder().skipResourceVersioning(false).build());
    assertThat(result).isFalse();
    result = k8sStepHelper.getSkipResourceVersioning(
        OpenshiftManifestOutcome.builder().skipResourceVersioning(true).build());
    assertThat(result).isTrue();
    result = k8sStepHelper.getSkipResourceVersioning(
        OpenshiftManifestOutcome.builder().skipResourceVersioning(false).build());
    assertThat(result).isFalse();

    result = k8sStepHelper.getSkipResourceVersioning(ValuesManifestOutcome.builder().build());
    assertThat(result).isFalse();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetManifestDelegateConfigForKustomize() {
    KustomizeManifestOutcome manifestOutcome =
        KustomizeManifestOutcome.builder()
            .store(GitStore.builder()
                       .branch(ParameterField.createValueField("test"))
                       .connectorRef(ParameterField.createValueField("org.connectorRef"))
                       .paths(ParameterField.createValueField(Arrays.asList("file1")))
                       .folderPath(ParameterField.createValueField("kustomize-dir"))
                       .build())
            .pluginPath("/usr/bin/kustomize")
            .build();

    doReturn(
        Optional.of(ConnectorResponseDTO.builder()
                        .connector(ConnectorInfoDTO.builder().connectorConfig(GitConfigDTO.builder().build()).build())
                        .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());

    ManifestDelegateConfig delegateConfig = k8sStepHelper.getManifestDelegateConfig(manifestOutcome, ambiance);
    assertThat(delegateConfig.getManifestType()).isEqualTo(ManifestType.KUSTOMIZE);
    assertThat(delegateConfig).isInstanceOf(KustomizeManifestDelegateConfig.class);
    assertThat(delegateConfig.getStoreDelegateConfig()).isNotNull();
    assertThat(delegateConfig.getStoreDelegateConfig()).isInstanceOf(GitStoreDelegateConfig.class);
    KustomizeManifestDelegateConfig kustomizeManifestDelegateConfig = (KustomizeManifestDelegateConfig) delegateConfig;
    assertThat(kustomizeManifestDelegateConfig.getPluginPath()).isEqualTo("/usr/bin/kustomize");
    assertThat(kustomizeManifestDelegateConfig.getKustomizeDirPath()).isEqualTo("kustomize-dir");
    assertThat(kustomizeManifestDelegateConfig.getStoreDelegateConfig()).isInstanceOf(GitStoreDelegateConfig.class);
    GitStoreDelegateConfig gitStoreDelegateConfig =
        (GitStoreDelegateConfig) kustomizeManifestDelegateConfig.getStoreDelegateConfig();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetManifestDelegateConfigForHelmChartUsingHttpRepo() {
    String connectorRef = "org.http_helm_connector";
    String chartName = "chartName";
    String chartVersion = "chartVersion";
    HttpHelmConnectorDTO httpHelmConnectorConfig =
        HttpHelmConnectorDTO.builder()
            .auth(HttpHelmAuthenticationDTO.builder().authType(HttpHelmAuthType.ANONYMOUS).build())
            .build();
    HelmChartManifestOutcome manifestOutcome =
        HelmChartManifestOutcome.builder()
            .store(HttpStoreConfig.builder().connectorRef(ParameterField.createValueField(connectorRef)).build())
            .chartName(chartName)
            .chartVersion(chartVersion)
            .build();

    doReturn(Optional.of(ConnectorResponseDTO.builder()
                             .connector(ConnectorInfoDTO.builder()
                                            .connectorType(HTTP_HELM_REPO)
                                            .connectorConfig(httpHelmConnectorConfig)
                                            .build())
                             .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());

    ManifestDelegateConfig delegateConfig = k8sStepHelper.getManifestDelegateConfig(manifestOutcome, ambiance);
    assertThat(delegateConfig.getManifestType()).isEqualTo(ManifestType.HELM_CHART);
    assertThat(delegateConfig).isInstanceOf(HelmChartManifestDelegateConfig.class);
    HelmChartManifestDelegateConfig helmChartDelegateConfig = (HelmChartManifestDelegateConfig) delegateConfig;
    assertThat(helmChartDelegateConfig.getStoreDelegateConfig()).isNotNull();
    assertThat(helmChartDelegateConfig.getStoreDelegateConfig()).isInstanceOf(HttpHelmStoreDelegateConfig.class);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldConvertGitAccountRepoWithRepoName() {
    List<String> paths = Arrays.asList("path/to");
    GitStoreConfig gitStoreConfig = GithubStore.builder()
                                        .repoName(ParameterField.createValueField("parent-repo/module"))
                                        .paths(ParameterField.createValueField(paths))
                                        .build();
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().build();
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();
    GitConfigDTO gitConfigDTO =
        GitConfigDTO.builder().gitConnectionType(GitConnectionType.ACCOUNT).url("http://localhost").build();
    GitStoreDelegateConfig gitStoreDelegateConfig =
        k8sStepHelper.getGitStoreDelegateConfig(gitStoreConfig, connectorInfoDTO, Collections.emptyList(),
            sshKeySpecDTO, gitConfigDTO, ManifestType.K8S_MANIFEST.name(), paths);
    assertThat(gitStoreDelegateConfig).isNotNull();
    assertThat(gitStoreDelegateConfig.getGitConfigDTO()).isInstanceOf(GitConfigDTO.class);
    GitConfigDTO convertedConfig = (GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO();
    assertThat(convertedConfig.getUrl()).isEqualTo("http://localhost/parent-repo/module");
    assertThat(convertedConfig.getGitConnectionType()).isEqualTo(GitConnectionType.REPO);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldNotConvertGitRepoWithRepoName() {
    List<String> paths = Arrays.asList("path/to");
    GitStoreConfig gitStoreConfig = GithubStore.builder()
                                        .repoName(ParameterField.createValueField("parent-repo/module"))
                                        .paths(ParameterField.createValueField(paths))
                                        .build();
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().build();
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();
    GitConfigDTO gitConfigDTO =
        GitConfigDTO.builder().gitConnectionType(GitConnectionType.REPO).url("http://localhost/repository").build();

    GitStoreDelegateConfig gitStoreDelegateConfig =
        k8sStepHelper.getGitStoreDelegateConfig(gitStoreConfig, connectorInfoDTO, Collections.emptyList(),
            sshKeySpecDTO, gitConfigDTO, ManifestType.K8S_MANIFEST.name(), paths);
    assertThat(gitStoreDelegateConfig).isNotNull();
    assertThat(gitStoreDelegateConfig.getGitConfigDTO()).isInstanceOf(GitConfigDTO.class);
    GitConfigDTO convertedConfig = (GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO();
    assertThat(convertedConfig.getUrl()).isEqualTo("http://localhost/repository");
    assertThat(convertedConfig.getGitConnectionType()).isEqualTo(GitConnectionType.REPO);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldFailGitRepoConversionIfRepoNameIsMissing() {
    List<String> paths = Arrays.asList("path/to");
    GitStoreConfig gitStoreConfig = GithubStore.builder().paths(ParameterField.createValueField(paths)).build();
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().build();
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();
    GitConfigDTO gitConfigDTO =
        GitConfigDTO.builder().gitConnectionType(GitConnectionType.ACCOUNT).url("http://localhost").build();

    try {
      k8sStepHelper.getGitStoreDelegateConfig(gitStoreConfig, connectorInfoDTO, Collections.emptyList(), sshKeySpecDTO,
          gitConfigDTO, ManifestType.K8S_MANIFEST.name(), paths);
    } catch (Exception thrown) {
      assertThat(thrown).isNotNull();
      assertThat(thrown).isInstanceOf(InvalidRequestException.class);
      assertThat(thrown.getMessage()).isEqualTo("Repo name cannot be empty for Account level git connector");
    }
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void shouldTrimFieldsForGetGitStoreDelegateConfig() {
    List<String> paths = Arrays.asList("test/path1", "test/path2 ", " test/path3", " test/path4 ", "te st/path5 ");
    GitStoreConfig gitStoreConfig = GithubStore.builder()
                                        .paths(ParameterField.createValueField(paths))
                                        .commitId(ParameterField.createValueField(" commitId "))
                                        .branch(ParameterField.createValueField(" branch "))
                                        .build();
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().build();
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();
    GitConfigDTO gitConfigDTO =
        GitConfigDTO.builder().gitConnectionType(GitConnectionType.REPO).url("http://localhost").build();

    GitStoreDelegateConfig gitStoreDelegateConfig =
        k8sStepHelper.getGitStoreDelegateConfig(gitStoreConfig, connectorInfoDTO, Collections.emptyList(),
            sshKeySpecDTO, gitConfigDTO, ManifestType.K8S_MANIFEST.name(), paths);

    assertThat(gitStoreDelegateConfig.getBranch()).isEqualTo("branch");
    assertThat(gitStoreDelegateConfig.getCommitId()).isEqualTo("commitId");
    assertThat(gitStoreDelegateConfig.getPaths())
        .containsExactlyInAnyOrder("test/path1", "test/path2", "test/path3", "test/path4", "te st/path5");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetManifestDelegateConfigForOpenshift() {
    OpenshiftManifestOutcome manifestOutcome =
        OpenshiftManifestOutcome.builder()
            .store(GitStore.builder()
                       .branch(ParameterField.createValueField("test"))
                       .connectorRef(ParameterField.createValueField("org.connectorRef"))
                       .paths(ParameterField.createValueField(Arrays.asList("file1", "file2")))
                       .build())
            .build();

    doReturn(
        Optional.of(ConnectorResponseDTO.builder()
                        .connector(ConnectorInfoDTO.builder().connectorConfig(GitConfigDTO.builder().build()).build())
                        .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());

    ManifestDelegateConfig delegateConfig = k8sStepHelper.getManifestDelegateConfig(manifestOutcome, ambiance);
    assertThat(delegateConfig.getManifestType()).isEqualTo(ManifestType.OPENSHIFT_TEMPLATE);
    assertThat(delegateConfig).isInstanceOf(OpenshiftManifestDelegateConfig.class);
    assertThat(delegateConfig.getStoreDelegateConfig()).isNotNull();
    assertThat(delegateConfig.getStoreDelegateConfig()).isInstanceOf(GitStoreDelegateConfig.class);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldRenderReversedValuesFilesForOpenshiftManifest() {
    String valueFile1 = "file1";
    String valueFile2 = "file2";
    List<String> valuesFiles = Arrays.asList(valueFile1, valueFile2);

    doReturn(valueFile1).when(engineExpressionService).renderExpression(any(), eq(valueFile1));
    doReturn(valueFile2).when(engineExpressionService).renderExpression(any(), eq(valueFile2));

    List<String> renderedValuesFiles = k8sStepHelper.renderValues(
        OpenshiftManifestOutcome.builder().build(), Ambiance.newBuilder().build(), valuesFiles);
    assertThat(renderedValuesFiles).isNotEmpty();
    assertThat(renderedValuesFiles).containsExactly(valueFile2, valueFile1);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testNamespaceValidation() {
    Ambiance ambiance = getAmbiance();
    ConnectorInfoDTO connectorDTO = ConnectorInfoDTO.builder().build();
    Optional<ConnectorResponseDTO> connectorDTOOptional =
        Optional.of(ConnectorResponseDTO.builder().connector(connectorDTO).build());
    doReturn(connectorDTOOptional).when(connectorService).get("account1", "org1", "project1", "abcConnector");

    K8sDirectInfrastructureOutcomeBuilder outcomeBuilder =
        K8sDirectInfrastructureOutcome.builder().connectorRef("abcConnector").namespace("namespace test");

    try {
      k8sStepHelper.getK8sInfraDelegateConfig(outcomeBuilder.build(), ambiance);
      fail("Should not reach here.");
    } catch (InvalidArgumentsException ex) {
      assertThat(ex.getParams().get("args"))
          .isEqualTo(
              "Namespace: \"namespace test\" is an invalid name. Namespaces may only contain lowercase letters, numbers, and '-'.");
    }

    try {
      outcomeBuilder.namespace("");
      k8sStepHelper.getK8sInfraDelegateConfig(outcomeBuilder.build(), ambiance);
      fail("Should not reach here.");
    } catch (InvalidArgumentsException ex) {
      assertThat(ex.getParams().get("args")).isEqualTo("Namespace: Namespace cannot be empty");
    }

    try {
      outcomeBuilder.namespace(" namespace test ");
      k8sStepHelper.getK8sInfraDelegateConfig(outcomeBuilder.build(), ambiance);
      fail("Should not reach here.");
    } catch (InvalidArgumentsException ex) {
      assertThat(ex.getParams().get("args"))
          .isEqualTo("Namespace: [ namespace test ] contains leading or trailing whitespaces");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetInfrastructureOutcome() {
    K8sDirectInfrastructureOutcome outcome = K8sDirectInfrastructureOutcome.builder().build();
    doReturn(OptionalOutcome.builder().outcome(outcome).found(true).build())
        .when(outcomeService)
        .resolveOptional(
            ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));
    assertThat(k8sStepHelper.getInfrastructureOutcome(ambiance)).isEqualTo(outcome);

    doReturn(OptionalOutcome.builder().found(false).build())
        .when(outcomeService)
        .resolveOptional(
            ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));
    assertThatThrownBy(() -> k8sStepHelper.getInfrastructureOutcome(ambiance))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(MISSING_INFRASTRUCTURE_ERROR);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetManifestDelegateConfigForHelmChartUsingAwsS3Repo() {
    String connectorRef = "org.aws_s3_repo";
    String bucketName = "bucketName";
    String region = "region";
    String folderPath = "basePath";
    String chartName = "chartName";
    String chartVersion = "chartVersion";
    AwsConnectorDTO awsConnectorConfig =
        AwsConnectorDTO.builder()
            .credential(AwsCredentialDTO.builder().awsCredentialType(AwsCredentialType.INHERIT_FROM_DELEGATE).build())
            .build();

    HelmChartManifestOutcome manifestOutcome =
        HelmChartManifestOutcome.builder()
            .store(S3StoreConfig.builder()
                       .connectorRef(ParameterField.createValueField(connectorRef))
                       .bucketName(ParameterField.createValueField(bucketName))
                       .region(ParameterField.createValueField(region))
                       .folderPath(ParameterField.createValueField(folderPath))
                       .build())
            .chartName(chartName)
            .chartVersion(chartVersion)
            .build();

    doReturn(
        Optional.of(
            ConnectorResponseDTO.builder()
                .connector(ConnectorInfoDTO.builder().connectorType(AWS).connectorConfig(awsConnectorConfig).build())
                .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());

    ManifestDelegateConfig delegateConfig = k8sStepHelper.getManifestDelegateConfig(manifestOutcome, ambiance);
    assertThat(delegateConfig.getManifestType()).isEqualTo(ManifestType.HELM_CHART);
    assertThat(delegateConfig).isInstanceOf(HelmChartManifestDelegateConfig.class);
    HelmChartManifestDelegateConfig helmChartDelegateConfig = (HelmChartManifestDelegateConfig) delegateConfig;
    assertThat(helmChartDelegateConfig.getStoreDelegateConfig()).isNotNull();
    assertThat(helmChartDelegateConfig.getStoreDelegateConfig()).isInstanceOf(S3HelmStoreDelegateConfig.class);
    S3HelmStoreDelegateConfig s3StoreDelegateConfig =
        (S3HelmStoreDelegateConfig) helmChartDelegateConfig.getStoreDelegateConfig();
    assertThat(s3StoreDelegateConfig.getBucketName()).isEqualTo(bucketName);
    assertThat(s3StoreDelegateConfig.getRegion()).isEqualTo(region);
    assertThat(s3StoreDelegateConfig.getFolderPath()).isEqualTo(folderPath);
    assertThat(s3StoreDelegateConfig.getAwsConnector()).isEqualTo(awsConnectorConfig);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetManifestDelegateConfigForHelmChartUsingGcsRepo() {
    String connectorRef = "org.aws_s3_repo";
    String bucketName = "bucketName";
    String folderPath = "basePath";
    String chartName = "chartName";
    String chartVersion = "chartVersion";
    GcpConnectorDTO gcpConnectorDTO =
        GcpConnectorDTO.builder()
            .credential(
                GcpConnectorCredentialDTO.builder().gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE).build())
            .build();

    HelmChartManifestOutcome helmChartManifestOutcome =
        HelmChartManifestOutcome.builder()
            .chartVersion(chartVersion)
            .chartName(chartName)
            .store(GcsStoreConfig.builder()
                       .connectorRef(ParameterField.createValueField(connectorRef))
                       .bucketName(ParameterField.createValueField(bucketName))
                       .folderPath(ParameterField.createValueField(folderPath))
                       .build())
            .build();

    doReturn(Optional.of(
                 ConnectorResponseDTO.builder()
                     .connector(ConnectorInfoDTO.builder().connectorType(GCP).connectorConfig(gcpConnectorDTO).build())
                     .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());

    ManifestDelegateConfig delegateConfig = k8sStepHelper.getManifestDelegateConfig(helmChartManifestOutcome, ambiance);
    assertThat(delegateConfig.getManifestType()).isEqualTo(ManifestType.HELM_CHART);
    assertThat(delegateConfig).isInstanceOf(HelmChartManifestDelegateConfig.class);
    HelmChartManifestDelegateConfig helmChartDelegateConfig = (HelmChartManifestDelegateConfig) delegateConfig;
    assertThat(helmChartDelegateConfig.getStoreDelegateConfig()).isNotNull();
    assertThat(helmChartDelegateConfig.getStoreDelegateConfig()).isInstanceOf(GcsHelmStoreDelegateConfig.class);
    GcsHelmStoreDelegateConfig gcsHelmStoreDelegateConfig =
        (GcsHelmStoreDelegateConfig) helmChartDelegateConfig.getStoreDelegateConfig();
    assertThat(gcsHelmStoreDelegateConfig.getBucketName()).isEqualTo(bucketName);
    assertThat(gcsHelmStoreDelegateConfig.getFolderPath()).isEqualTo(folderPath);
    assertThat(gcsHelmStoreDelegateConfig.getGcpConnector()).isEqualTo(gcpConnectorDTO);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetTimeoutValue() {
    StepElementParameters definedValue =
        StepElementParameters.builder().timeout(ParameterField.createValueField("15m")).build();
    StepElementParameters nullValue = StepElementParameters.builder().timeout(ParameterField.ofNull()).build();
    assertThat(K8sStepHelper.getTimeoutValue(definedValue)).isEqualTo("15m");
    assertThat(K8sStepHelper.getTimeoutValue(nullValue)).isEqualTo("10m");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetTimeoutInMin() {
    StepElementParameters value =
        StepElementParameters.builder().timeout(ParameterField.createValueField("15m")).build();
    assertThat(K8sStepHelper.getTimeoutInMin(value)).isEqualTo(15);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetTimeoutInMillis() {
    StepElementParameters value =
        StepElementParameters.builder().timeout(ParameterField.createValueField("15m")).build();
    assertThat(K8sStepHelper.getTimeoutInMillis(value)).isEqualTo(900000);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldPrepareK8sGitValuesFetchTask() {
    K8sDirectInfrastructureOutcome k8sDirectInfrastructureOutcome =
        K8sDirectInfrastructureOutcome.builder().namespace("default").build();
    GitStore gitStore = GitStore.builder()
                            .branch(ParameterField.createValueField("master"))
                            .paths(ParameterField.createValueField(Arrays.asList("path/to/k8s/manifest")))
                            .connectorRef(ParameterField.createValueField("git-connector"))
                            .build();
    K8sManifestOutcome k8sManifestOutcome = K8sManifestOutcome.builder().identifier("k8s").store(gitStore).build();
    Map<String, ManifestOutcome> manifestOutcomeMap = ImmutableMap.of("k8s", k8sManifestOutcome);
    RefObject manifests = RefObject.newBuilder()
                              .setName(OutcomeExpressionConstants.MANIFESTS)
                              .setKey(OutcomeExpressionConstants.MANIFESTS)
                              .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                              .build();

    RefObject infra = RefObject.newBuilder()
                          .setName(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setKey(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                          .build();

    StepElementParameters rollingStepElementParams =
        StepElementParameters.builder().spec(K8sRollingStepParameters.infoBuilder().build()).build();
    doReturn(new ManifestsOutcome(manifestOutcomeMap)).when(outcomeService).resolve(eq(ambiance), eq(manifests));
    doReturn(k8sDirectInfrastructureOutcome).when(outcomeService).resolve(eq(ambiance), eq(infra));

    doReturn(
        Optional.of(ConnectorResponseDTO.builder()
                        .connector(ConnectorInfoDTO.builder()
                                       .connectorConfig(GitConfigDTO.builder().gitAuthType(GitAuthType.HTTP).build())
                                       .name("test")
                                       .build())

                        .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());

    TaskChainResponse taskChainResponse =
        k8sStepHelper.startChainLink(k8sStepExecutor, ambiance, rollingStepElementParams);
    assertThat(taskChainResponse).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isInstanceOf(K8sStepPassThroughData.class);
    K8sStepPassThroughData k8sStepPassThroughData = (K8sStepPassThroughData) taskChainResponse.getPassThroughData();
    assertThat(k8sStepPassThroughData.getValuesManifestOutcomes()).isNotEmpty();
    assertThat(k8sStepPassThroughData.getValuesManifestOutcomes().size()).isEqualTo(1);
    ValuesManifestOutcome valuesManifestOutcome = k8sStepPassThroughData.getValuesManifestOutcomes().get(0);
    assertThat(valuesManifestOutcome.getIdentifier()).isEqualTo(k8sManifestOutcome.getIdentifier());
    assertThat(valuesManifestOutcome.getStore()).isEqualTo(k8sManifestOutcome.getStore());
    ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
    verify(kryoSerializer, times(2)).asDeflatedBytes(argumentCaptor.capture());
    TaskParameters taskParameters = (TaskParameters) argumentCaptor.getAllValues().get(0);
    assertThat(taskParameters).isInstanceOf(GitFetchRequest.class);
    GitFetchRequest gitFetchRequest = (GitFetchRequest) taskParameters;
    assertThat(gitFetchRequest.getGitFetchFilesConfigs()).isNotEmpty();
    assertThat(gitFetchRequest.getGitFetchFilesConfigs().size()).isEqualTo(1);
    GitFetchFilesConfig gitFetchFilesConfig = gitFetchRequest.getGitFetchFilesConfigs().get(0);
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths()).isNotEmpty();
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths().size()).isEqualTo(1);
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths().get(0))
        .isEqualTo("path/to/k8s/manifest/values.yaml");
    assertThat(argumentCaptor.getAllValues().get(1)).isInstanceOf(HttpConnectionExecutionCapability.class);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldPrepareHelmGitValuesFetchTask() {
    K8sDirectInfrastructureOutcome k8sDirectInfrastructureOutcome =
        K8sDirectInfrastructureOutcome.builder().namespace("default").build();
    GitStore gitStore = GitStore.builder()
                            .branch(ParameterField.createValueField("master"))
                            .folderPath(ParameterField.createValueField("path/to/helm/chart"))
                            .connectorRef(ParameterField.createValueField("git-connector"))
                            .build();
    HelmChartManifestOutcome helmChartManifestOutcome =
        HelmChartManifestOutcome.builder().identifier("helm").store(gitStore).build();
    Map<String, ManifestOutcome> manifestOutcomeMap = ImmutableMap.of("k8s", helmChartManifestOutcome);
    RefObject manifests = RefObject.newBuilder()
                              .setName(OutcomeExpressionConstants.MANIFESTS)
                              .setKey(OutcomeExpressionConstants.MANIFESTS)
                              .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                              .build();

    RefObject infra = RefObject.newBuilder()
                          .setName(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setKey(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                          .build();

    StepElementParameters rollingStepElementParams =
        StepElementParameters.builder().spec(K8sRollingStepParameters.infoBuilder().build()).build();
    doReturn(new ManifestsOutcome(manifestOutcomeMap)).when(outcomeService).resolve(eq(ambiance), eq(manifests));
    doReturn(k8sDirectInfrastructureOutcome).when(outcomeService).resolve(eq(ambiance), eq(infra));

    doReturn(
        Optional.of(ConnectorResponseDTO.builder()
                        .connector(ConnectorInfoDTO.builder()
                                       .connectorConfig(GitConfigDTO.builder().gitAuthType(GitAuthType.HTTP).build())
                                       .name("test")
                                       .build())
                        .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());

    TaskChainResponse taskChainResponse =
        k8sStepHelper.startChainLink(k8sStepExecutor, ambiance, rollingStepElementParams);
    assertThat(taskChainResponse).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isInstanceOf(K8sStepPassThroughData.class);
    K8sStepPassThroughData k8sStepPassThroughData = (K8sStepPassThroughData) taskChainResponse.getPassThroughData();
    assertThat(k8sStepPassThroughData.getValuesManifestOutcomes()).isNotEmpty();
    assertThat(k8sStepPassThroughData.getValuesManifestOutcomes().size()).isEqualTo(1);
    ValuesManifestOutcome valuesManifestOutcome = k8sStepPassThroughData.getValuesManifestOutcomes().get(0);
    assertThat(valuesManifestOutcome.getIdentifier()).isEqualTo(helmChartManifestOutcome.getIdentifier());
    assertThat(valuesManifestOutcome.getStore()).isEqualTo(helmChartManifestOutcome.getStore());
    ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
    verify(kryoSerializer, times(2)).asDeflatedBytes(argumentCaptor.capture());
    TaskParameters taskParameters = (TaskParameters) argumentCaptor.getAllValues().get(0);
    assertThat(taskParameters).isInstanceOf(GitFetchRequest.class);
    GitFetchRequest gitFetchRequest = (GitFetchRequest) taskParameters;
    assertThat(gitFetchRequest.getGitFetchFilesConfigs()).isNotEmpty();
    assertThat(gitFetchRequest.getGitFetchFilesConfigs().size()).isEqualTo(1);
    GitFetchFilesConfig gitFetchFilesConfig = gitFetchRequest.getGitFetchFilesConfigs().get(0);
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths()).isNotEmpty();
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths().size()).isEqualTo(1);
    assertThat(gitFetchFilesConfig.getGitStoreDelegateConfig().getPaths().get(0))
        .isEqualTo("path/to/helm/chart/values.yaml");
    assertThat(argumentCaptor.getAllValues().get(1)).isInstanceOf(HttpConnectionExecutionCapability.class);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldPrepareHelmS3ValuesFetchTask() {
    K8sDirectInfrastructureOutcome k8sDirectInfrastructureOutcome =
        K8sDirectInfrastructureOutcome.builder().namespace("default").build();
    S3StoreConfig s3Store = S3StoreConfig.builder()
                                .bucketName(ParameterField.createValueField("bucket"))
                                .region(ParameterField.createValueField("us-east-1"))
                                .folderPath(ParameterField.createValueField("path/to/helm/chart"))
                                .connectorRef(ParameterField.createValueField("aws-connector"))
                                .build();

    HelmChartManifestOutcome helmChartManifestOutcome =
        HelmChartManifestOutcome.builder().identifier("helm").store(s3Store).build();
    Map<String, ManifestOutcome> manifestOutcomeMap = ImmutableMap.of("k8s", helmChartManifestOutcome);
    RefObject manifests = RefObject.newBuilder()
                              .setName(OutcomeExpressionConstants.MANIFESTS)
                              .setKey(OutcomeExpressionConstants.MANIFESTS)
                              .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                              .build();

    RefObject infra = RefObject.newBuilder()
                          .setName(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setKey(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                          .build();

    StepElementParameters rollingStepElementParams =
        StepElementParameters.builder().spec(K8sRollingStepParameters.infoBuilder().build()).build();
    doReturn(new ManifestsOutcome(manifestOutcomeMap)).when(outcomeService).resolve(eq(ambiance), eq(manifests));
    doReturn(k8sDirectInfrastructureOutcome).when(outcomeService).resolve(eq(ambiance), eq(infra));

    doReturn(Optional.of(
                 ConnectorResponseDTO.builder()
                     .connector(ConnectorInfoDTO.builder()
                                    .connectorConfig(
                                        AwsConnectorDTO.builder()
                                            .credential(AwsCredentialDTO.builder()
                                                            .awsCredentialType(AwsCredentialType.INHERIT_FROM_DELEGATE)
                                                            .build())
                                            .build())
                                    .name("helm-s3-repo-display")
                                    .identifier("helm-s3-repo")
                                    .connectorType(AWS)
                                    .build())
                     .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());

    TaskChainResponse taskChainResponse =
        k8sStepHelper.startChainLink(k8sStepExecutor, ambiance, rollingStepElementParams);
    assertThat(taskChainResponse).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isInstanceOf(K8sStepPassThroughData.class);
    ArgumentCaptor<TaskParameters> taskParametersArgumentCaptor = ArgumentCaptor.forClass(TaskParameters.class);
    verify(kryoSerializer).asDeflatedBytes(taskParametersArgumentCaptor.capture());
    TaskParameters taskParameters = taskParametersArgumentCaptor.getValue();
    assertThat(taskParameters).isInstanceOf(HelmValuesFetchRequest.class);
    HelmValuesFetchRequest helmValuesFetchRequest = (HelmValuesFetchRequest) taskParameters;
    assertThat(helmValuesFetchRequest.getTimeout()).isNotNull();
    assertThat(helmValuesFetchRequest.getHelmChartManifestDelegateConfig().getStoreDelegateConfig())
        .isInstanceOf(S3HelmStoreDelegateConfig.class);
    S3HelmStoreDelegateConfig s3StoreConfig =
        (S3HelmStoreDelegateConfig) helmValuesFetchRequest.getHelmChartManifestDelegateConfig()
            .getStoreDelegateConfig();
    assertThat(s3StoreConfig.getBucketName()).isEqualTo("bucket");
    assertThat(s3StoreConfig.getRegion()).isEqualTo("us-east-1");
    assertThat(s3StoreConfig.getFolderPath()).isEqualTo("path/to/helm/chart");
    assertThat(s3StoreConfig.getRepoName()).isEqualTo("helm-s3-repo");
    assertThat(s3StoreConfig.getRepoDisplayName()).isEqualTo("helm-s3-repo-display");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldPrepareHelmGcsValuesFetchTask() {
    K8sDirectInfrastructureOutcome k8sDirectInfrastructureOutcome =
        K8sDirectInfrastructureOutcome.builder().namespace("default").build();
    GcsStoreConfig gcsStore = GcsStoreConfig.builder()
                                  .bucketName(ParameterField.createValueField("bucket"))
                                  .folderPath(ParameterField.createValueField("path/to/helm/chart"))
                                  .connectorRef(ParameterField.createValueField("gcs-connector"))
                                  .build();

    HelmChartManifestOutcome helmChartManifestOutcome =
        HelmChartManifestOutcome.builder().identifier("helm").store(gcsStore).build();
    Map<String, ManifestOutcome> manifestOutcomeMap = ImmutableMap.of("k8s", helmChartManifestOutcome);
    RefObject manifests = RefObject.newBuilder()
                              .setName(OutcomeExpressionConstants.MANIFESTS)
                              .setKey(OutcomeExpressionConstants.MANIFESTS)
                              .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                              .build();

    RefObject infra = RefObject.newBuilder()
                          .setName(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setKey(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                          .build();

    StepElementParameters rollingStepElementParams =
        StepElementParameters.builder().spec(K8sRollingStepParameters.infoBuilder().build()).build();
    doReturn(new ManifestsOutcome(manifestOutcomeMap)).when(outcomeService).resolve(eq(ambiance), eq(manifests));
    doReturn(k8sDirectInfrastructureOutcome).when(outcomeService).resolve(eq(ambiance), eq(infra));

    doReturn(Optional.of(
                 ConnectorResponseDTO.builder()
                     .connector(ConnectorInfoDTO.builder()
                                    .connectorConfig(
                                        GcpConnectorDTO.builder()
                                            .credential(GcpConnectorCredentialDTO.builder()
                                                            .gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
                                                            .build())
                                            .build())
                                    .name("helm-gcs-repo-display")
                                    .identifier("helm-gcs-repo")
                                    .connectorType(GCP)
                                    .build())
                     .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());

    TaskChainResponse taskChainResponse =
        k8sStepHelper.startChainLink(k8sStepExecutor, ambiance, rollingStepElementParams);
    assertThat(taskChainResponse).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isInstanceOf(K8sStepPassThroughData.class);
    ArgumentCaptor<TaskParameters> taskParametersArgumentCaptor = ArgumentCaptor.forClass(TaskParameters.class);
    verify(kryoSerializer).asDeflatedBytes(taskParametersArgumentCaptor.capture());
    TaskParameters taskParameters = taskParametersArgumentCaptor.getValue();
    assertThat(taskParameters).isInstanceOf(HelmValuesFetchRequest.class);
    HelmValuesFetchRequest helmValuesFetchRequest = (HelmValuesFetchRequest) taskParameters;
    assertThat(helmValuesFetchRequest.getTimeout()).isNotNull();
    assertThat(helmValuesFetchRequest.getHelmChartManifestDelegateConfig().getStoreDelegateConfig())
        .isInstanceOf(GcsHelmStoreDelegateConfig.class);
    GcsHelmStoreDelegateConfig gcsStoreConfig =
        (GcsHelmStoreDelegateConfig) helmValuesFetchRequest.getHelmChartManifestDelegateConfig()
            .getStoreDelegateConfig();
    assertThat(gcsStoreConfig.getBucketName()).isEqualTo("bucket");
    assertThat(gcsStoreConfig.getFolderPath()).isEqualTo("path/to/helm/chart");
    assertThat(gcsStoreConfig.getRepoName()).isEqualTo("helm-gcs-repo");
    assertThat(gcsStoreConfig.getRepoDisplayName()).isEqualTo("helm-gcs-repo-display");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldPrepareHelmHttpValuesFetchTask() {
    K8sDirectInfrastructureOutcome k8sDirectInfrastructureOutcome =
        K8sDirectInfrastructureOutcome.builder().namespace("default").build();
    HttpStoreConfig httpStore =
        HttpStoreConfig.builder().connectorRef(ParameterField.createValueField("http-connector")).build();

    HelmChartManifestOutcome helmChartManifestOutcome =
        HelmChartManifestOutcome.builder().identifier("helm").store(httpStore).build();
    Map<String, ManifestOutcome> manifestOutcomeMap = ImmutableMap.of("k8s", helmChartManifestOutcome);
    RefObject manifests = RefObject.newBuilder()
                              .setName(OutcomeExpressionConstants.MANIFESTS)
                              .setKey(OutcomeExpressionConstants.MANIFESTS)
                              .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                              .build();

    RefObject infra = RefObject.newBuilder()
                          .setName(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setKey(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                          .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                          .build();

    StepElementParameters rollingStepElementParams =
        StepElementParameters.builder().spec(K8sRollingStepParameters.infoBuilder().build()).build();
    doReturn(new ManifestsOutcome(manifestOutcomeMap)).when(outcomeService).resolve(eq(ambiance), eq(manifests));
    doReturn(k8sDirectInfrastructureOutcome).when(outcomeService).resolve(eq(ambiance), eq(infra));

    doReturn(
        Optional.of(
            ConnectorResponseDTO.builder()
                .connector(
                    ConnectorInfoDTO.builder()
                        .connectorConfig(
                            HttpHelmConnectorDTO.builder()
                                .auth(HttpHelmAuthenticationDTO.builder().authType(HttpHelmAuthType.ANONYMOUS).build())
                                .build())
                        .name("helm-http-repo-display")
                        .identifier("helm-http-repo")
                        .connectorType(HTTP_HELM_REPO)
                        .build())
                .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());

    TaskChainResponse taskChainResponse =
        k8sStepHelper.startChainLink(k8sStepExecutor, ambiance, rollingStepElementParams);
    assertThat(taskChainResponse).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isNotNull();
    assertThat(taskChainResponse.getPassThroughData()).isInstanceOf(K8sStepPassThroughData.class);
    ArgumentCaptor<TaskParameters> taskParametersArgumentCaptor = ArgumentCaptor.forClass(TaskParameters.class);
    verify(kryoSerializer).asDeflatedBytes(taskParametersArgumentCaptor.capture());
    TaskParameters taskParameters = taskParametersArgumentCaptor.getValue();
    assertThat(taskParameters).isInstanceOf(HelmValuesFetchRequest.class);
    HelmValuesFetchRequest helmValuesFetchRequest = (HelmValuesFetchRequest) taskParameters;
    assertThat(helmValuesFetchRequest.getTimeout()).isNotNull();
    assertThat(helmValuesFetchRequest.getHelmChartManifestDelegateConfig().getStoreDelegateConfig())
        .isInstanceOf(HttpHelmStoreDelegateConfig.class);
    HttpHelmStoreDelegateConfig httpStoreConfig =
        (HttpHelmStoreDelegateConfig) helmValuesFetchRequest.getHelmChartManifestDelegateConfig()
            .getStoreDelegateConfig();
    assertThat(httpStoreConfig.getRepoName()).isEqualTo("helm-http-repo");
    assertThat(httpStoreConfig.getRepoDisplayName()).isEqualTo("helm-http-repo-display");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldHandleHelmValueFetchResponse() throws Exception {
    StepElementParameters rollingStepElementParams =
        StepElementParameters.builder().spec(K8sRollingStepParameters.infoBuilder().build()).build();

    K8sStepPassThroughData passThroughData = K8sStepPassThroughData.builder()
                                                 .k8sManifestOutcome(K8sManifestOutcome.builder().build())
                                                 .infrastructure(K8sDirectInfrastructureOutcome.builder().build())
                                                 .build();

    HelmValuesFetchResponse helmValuesFetchResponse = HelmValuesFetchResponse.builder()
                                                          .valuesFileContent("values yaml payload")
                                                          .commandExecutionStatus(SUCCESS)
                                                          .build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of("helm-value-fetch-response", helmValuesFetchResponse);
    ThrowingSupplier responseDataSuplier = StrategyHelper.buildResponseDataSupplier(responseDataMap);

    k8sStepHelper.executeNextLink(
        k8sStepExecutor, ambiance, rollingStepElementParams, passThroughData, responseDataSuplier);

    ArgumentCaptor<List> valuesFilesContentCaptor = ArgumentCaptor.forClass(List.class);
    verify(k8sStepExecutor, times(1))
        .executeK8sTask(eq(passThroughData.getK8sManifestOutcome()), eq(ambiance), eq(rollingStepElementParams),
            valuesFilesContentCaptor.capture(), eq(passThroughData.getInfrastructure()), eq(false));

    List<String> valuesFilesContent = valuesFilesContentCaptor.getValue();
    assertThat(valuesFilesContent).isNotEmpty();
    assertThat(valuesFilesContent.get(0)).isEqualTo("values yaml payload");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldHandleHelmValueFetchResponseFailure() throws Exception {
    StepElementParameters rollingStepElementParams =
        StepElementParameters.builder().spec(K8sRollingStepParameters.infoBuilder().build()).build();

    K8sStepPassThroughData passThroughData = K8sStepPassThroughData.builder()
                                                 .k8sManifestOutcome(K8sManifestOutcome.builder().build())
                                                 .infrastructure(K8sDirectInfrastructureOutcome.builder().build())
                                                 .build();

    HelmValuesFetchResponse helmValuesFetchResponse =
        HelmValuesFetchResponse.builder().commandExecutionStatus(FAILURE).errorMessage("Something went wrong").build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of("helm-value-fetch-response", helmValuesFetchResponse);
    ThrowingSupplier responseDataSuplier = StrategyHelper.buildResponseDataSupplier(responseDataMap);

    TaskChainResponse response = k8sStepHelper.executeNextLink(
        k8sStepExecutor, ambiance, rollingStepElementParams, passThroughData, responseDataSuplier);

    assertThat(response.getPassThroughData()).isNotNull();
    assertThat(response.isChainEnd()).isTrue();
    assertThat(response.getPassThroughData()).isInstanceOf(HelmValuesFetchResponsePassThroughData.class);
    HelmValuesFetchResponsePassThroughData helmPassThroughData =
        (HelmValuesFetchResponsePassThroughData) response.getPassThroughData();
    assertThat(helmPassThroughData.getErrorMsg()).isEqualTo("Something went wrong");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteNextLinkInternalStepException() throws Exception {
    LogCallback mockLogCallback = mock(LogCallback.class);
    K8sStepHelper spyK8sStepHelper = spy(k8sStepHelper);

    StepElementParameters rollingStepElementParams =
        StepElementParameters.builder().spec(K8sRollingStepParameters.infoBuilder().build()).build();
    UnitProgressData unitProgressData =
        UnitProgressData.builder()
            .unitProgresses(Arrays.asList(
                UnitProgress.newBuilder().setUnitName("Fetch Files").setStatus(UnitStatus.RUNNING).build(),
                UnitProgress.newBuilder().setUnitName("Some Unit").setStatus(UnitStatus.SUCCESS).build()))
            .build();

    K8sStepPassThroughData passThroughData = K8sStepPassThroughData.builder()
                                                 .k8sManifestOutcome(K8sManifestOutcome.builder().build())
                                                 .infrastructure(K8sDirectInfrastructureOutcome.builder().build())
                                                 .build();

    GitFetchResponse gitFetchResponse = GitFetchResponse.builder()
                                            .filesFromMultipleRepo(Collections.emptyMap())
                                            .taskStatus(TaskStatus.SUCCESS)
                                            .unitProgressData(unitProgressData)
                                            .build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of("git-fetch-response", gitFetchResponse);
    ThrowingSupplier responseDataSuplier = StrategyHelper.buildResponseDataSupplier(responseDataMap);
    RuntimeException thrownException = new RuntimeException("Failed to do something");

    doThrow(thrownException)
        .when(k8sStepExecutor)
        .executeK8sTask(passThroughData.getK8sManifestOutcome(), ambiance, rollingStepElementParams,
            Collections.emptyList(), passThroughData.getInfrastructure(), false);
    doReturn(mockLogCallback).when(spyK8sStepHelper).getLogCallback("Fetch Files", ambiance, false);

    TaskChainResponse response = spyK8sStepHelper.executeNextLink(
        k8sStepExecutor, ambiance, rollingStepElementParams, passThroughData, responseDataSuplier);

    assertThat(response.getPassThroughData()).isInstanceOf(StepExceptionPassThroughData.class);
    StepExceptionPassThroughData stepExceptionData = (StepExceptionPassThroughData) response.getPassThroughData();
    assertThat(stepExceptionData.getErrorMessage()).isEqualTo(ExceptionUtils.getMessage(thrownException));
    List<UnitProgress> unitProgresses = stepExceptionData.getUnitProgressData().getUnitProgresses();
    assertThat(unitProgresses).hasSize(2);
    assertThat(unitProgresses.get(0).getEndTime()).isNotZero();
    assertThat(unitProgresses.get(0).getStatus()).isEqualTo(UnitStatus.FAILURE);

    verify(mockLogCallback, times(1))
        .saveExecutionLog(ExceptionUtils.getMessage(thrownException), LogLevel.ERROR, FAILURE);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleStepExceptionFailure() {
    List<UnitProgress> progressList = Collections.singletonList(UnitProgress.newBuilder().build());
    StepExceptionPassThroughData data =
        StepExceptionPassThroughData.builder()
            .unitProgressData(UnitProgressData.builder().unitProgresses(progressList).build())
            .errorMessage("Something went wrong")
            .build();

    StepResponse result = k8sStepHelper.handleStepExceptionFailure(data);

    assertThat(result.getUnitProgressList()).isEqualTo(progressList);
    assertThat(result.getStatus()).isEqualTo(Status.FAILED);
    assertThat(result.getFailureInfo().getFailureDataList()).hasSize(1);
    FailureData failureData = result.getFailureInfo().getFailureData(0);
    assertThat(failureData.getFailureTypesList()).contains(FailureType.APPLICATION_FAILURE);
    assertThat(failureData.getCode()).isEqualTo(GENERAL_ERROR.name());
    assertThat(failureData.getMessage()).isEqualTo("Something went wrong");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetParameterFieldBooleanValue() {
    assertThat(K8sStepHelper.getParameterFieldBooleanValue(
                   ParameterField.createValueField("true"), "testField", StepElementParameters.builder().build()))
        .isTrue();
    assertThat(K8sStepHelper.getParameterFieldBooleanValue(
                   ParameterField.createValueField("false"), "testField", StepElementParameters.builder().build()))
        .isFalse();

    assertThatThrownBy(()
                           -> K8sStepHelper.getParameterFieldBooleanValue(ParameterField.createValueField("absad"),
                               "testField", StepElementParameters.builder().identifier("test").type("Test").build()))
        .hasMessageContaining("for field testField in Test step with identifier: test");
  }
}