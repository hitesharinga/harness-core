package software.wings.sm.states;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder.aContainerServiceElement;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.DockerArtifactStream.Builder.aDockerArtifactStream;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.DOCKER_IMAGE;
import static software.wings.utils.WingsTestConstants.DOCKER_SOURCE;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.STATE_NAME;
import static software.wings.utils.WingsTestConstants.TASK_FAMILY;
import static software.wings.utils.WingsTestConstants.TASK_REVISION;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.Lists;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mongodb.morphia.Key;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.DockerConfig;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.exception.WingsException;
import software.wings.service.impl.security.EncryptionServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.EcsConvention;

import java.util.Collections;
import java.util.Date;

/**
 * Created by rishi on 2/27/17.
 */
public class EcsServiceSetupTest extends WingsBaseTest {
  @Mock private AwsClusterService awsClusterService;
  @Mock private SettingsService settingsService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactService artifactService;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Mock private KmsService kmsService;
  @Mock private WorkflowExecutionService workflowExecutionService;

  private WorkflowStandardParams workflowStandardParams = aWorkflowStandardParams()
                                                              .withAppId(APP_ID)
                                                              .withEnvId(ENV_ID)
                                                              .withArtifactIds(Lists.newArrayList(ARTIFACT_ID))
                                                              .build();
  private ServiceElement serviceElement = aServiceElement().withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  private PhaseElement phaseElement = aPhaseElement()
                                          .withUuid(getUuid())
                                          .withServiceElement(serviceElement)
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .build();
  private StateExecutionInstance stateExecutionInstance =
      aStateExecutionInstance()
          .withStateName(STATE_NAME)
          .addContextElement(workflowStandardParams)
          .addContextElement(phaseElement)
          .addContextElement(aContainerServiceElement()
                                 .withUuid(serviceElement.getUuid())
                                 .withResizeStrategy(RESIZE_NEW_FIRST)
                                 .build())
          .addStateExecutionData(new PhaseStepExecutionData())
          .build();
  private ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);

  private EcsServiceSetup ecsServiceSetup = new EcsServiceSetup(STATE_NAME);

  private Artifact artifact = anArtifact()
                                  .withAppId(APP_ID)
                                  .withServiceIds(Lists.newArrayList(SERVICE_ID))
                                  .withUuid(ARTIFACT_ID)
                                  .withArtifactStreamId(ARTIFACT_STREAM_ID)
                                  .build();
  private ArtifactStream artifactStream = aDockerArtifactStream()
                                              .withImageName(DOCKER_IMAGE)
                                              .withSourceName(DOCKER_SOURCE)
                                              .withSettingId(SETTING_ID)
                                              .build();
  private Application app = anApplication().withUuid(APP_ID).withName(APP_NAME).build();
  private Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build();
  private Service service = aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  private SettingAttribute computeProvider = aSettingAttribute().build();
  private SettingAttribute dockerConfigSettingAttribute = aSettingAttribute()
                                                              .withValue(DockerConfig.builder()
                                                                             .dockerRegistryUrl("url")
                                                                             .username("name")
                                                                             .password("pass".toCharArray())
                                                                             .accountId(ACCOUNT_ID)
                                                                             .build())
                                                              .build();
  private TaskDefinition taskDefinition;

  /**
   * Set up.
   */
  @Before
  public void setup() {
    when(appService.get(APP_ID)).thenReturn(app);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(env);
    when(artifactService.get(APP_ID, ARTIFACT_ID)).thenReturn(artifact);
    on(workflowStandardParams).set("artifactService", artifactService);
    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);

    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactStream);
    on(ecsServiceSetup).set("awsClusterService", awsClusterService);
    on(ecsServiceSetup).set("settingsService", settingsService);
    on(ecsServiceSetup).set("serviceResourceService", serviceResourceService);
    on(ecsServiceSetup).set("infrastructureMappingService", infrastructureMappingService);
    on(ecsServiceSetup).set("artifactStreamService", artifactStreamService);

    when(settingsService.get(APP_ID, COMPUTE_PROVIDER_ID)).thenReturn(computeProvider);
    when(settingsService.get(SETTING_ID)).thenReturn(dockerConfigSettingAttribute);
    when(settingsService.get(COMPUTE_PROVIDER_ID)).thenReturn(computeProvider);

    taskDefinition = new TaskDefinition();
    taskDefinition.setFamily(TASK_FAMILY);
    taskDefinition.setRevision(TASK_REVISION);

    when(awsClusterService.createTask(eq(Regions.US_EAST_1.getName()), any(SettingAttribute.class), any(),
             any(RegisterTaskDefinitionRequest.class)))
        .thenReturn(taskDefinition);

    when(serviceTemplateService.getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID))
        .thenReturn(singletonList(new Key<>(ServiceTemplate.class, "serviceTemplate", TEMPLATE_ID)));

    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(aServiceTemplate().withUuid(TEMPLATE_ID).build());
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID)).thenReturn(emptyList());
    when(kmsService.getEncryptionDetails(anyObject(), anyString())).thenReturn(Collections.emptyList());
    setInternalState(ecsServiceSetup, "kmsService", kmsService);
    setInternalState(ecsServiceSetup, "encryptionService", new EncryptionServiceImpl());
    setInternalState(ecsServiceSetup, "settingsService", settingsService);
    when(workflowExecutionService.getExecutionDetails(anyString(), anyString()))
        .thenReturn(aWorkflowExecution().build());
    setInternalState(context, "workflowExecutionService", workflowExecutionService);
  }

  @Test
  public void shouldThrowInvalidRequest() {
    try {
      ecsServiceSetup.execute(context);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).hasSize(1).containsKey("message");
      assertThat(exception.getParams().get("message")).asString().contains("Invalid infrastructure type");
    }
  }

  @Test
  public void shouldExecuteWithLastService() {
    on(context).set("serviceTemplateService", serviceTemplateService);

    InfrastructureMapping infrastructureMapping = anEcsInfrastructureMapping()
                                                      .withClusterName(CLUSTER_NAME)
                                                      .withAppId(APP_ID)
                                                      .withRegion(Regions.US_EAST_1.getName())
                                                      .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                      .build();
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);
    on(ecsServiceSetup).set("infrastructureMappingService", infrastructureMappingService);

    com.amazonaws.services.ecs.model.Service ecsService = new com.amazonaws.services.ecs.model.Service();
    ecsService.setServiceName(EcsConvention.getServiceName(taskDefinition.getFamily(), taskDefinition.getRevision()));
    ecsService.setCreatedAt(new Date());

    when(awsClusterService.getServices(
             Regions.US_EAST_1.getName(), computeProvider, Collections.emptyList(), CLUSTER_NAME))
        .thenReturn(Lists.newArrayList(ecsService));
    ExecutionResponse response = ecsServiceSetup.execute(context);
    assertThat(response).isNotNull();
    verify(awsClusterService)
        .createTask(eq(Regions.US_EAST_1.getName()), any(SettingAttribute.class), any(),
            any(RegisterTaskDefinitionRequest.class));
    verify(awsClusterService)
        .createService(
            eq(Regions.US_EAST_1.getName()), any(SettingAttribute.class), any(), any(CreateServiceRequest.class));
  }
}
