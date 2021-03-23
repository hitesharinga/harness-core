package software.wings.service.impl.yaml.handler.deploymentspec.userdata;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.exception.HarnessException;

import software.wings.api.DeploymentType;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.container.UserDataSpecificationYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 1/7/17
 */
@Singleton
public class UserDataSpecificationYamlHandler
    extends DeploymentSpecificationYamlHandler<UserDataSpecificationYaml, UserDataSpecification> {
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlHelper yamlHelper;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public UserDataSpecificationYaml toYaml(UserDataSpecification bean, String appId) {
    return UserDataSpecificationYaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(DeploymentType.AMI.name())
        .data(bean.getData())
        .build();
  }

  @Override
  public UserDataSpecification upsertFromYaml(ChangeContext<UserDataSpecificationYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    UserDataSpecification previous =
        get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    UserDataSpecification userDataSpecification = toBean(changeContext);
    userDataSpecification.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      userDataSpecification.setUuid(previous.getUuid());
      return serviceResourceService.updateUserDataSpecification(userDataSpecification);
    } else {
      return serviceResourceService.createUserDataSpecification(userDataSpecification);
    }
  }

  private UserDataSpecification toBean(ChangeContext<UserDataSpecificationYaml> changeContext) throws HarnessException {
    UserDataSpecificationYaml yaml = changeContext.getYaml();

    String filePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), filePath);
    notNullCheck("Could not lookup app for the yaml file: " + filePath, appId, USER);

    String serviceId = yamlHelper.getServiceId(appId, filePath);
    notNullCheck("Could not lookup service for the yaml file: " + filePath, serviceId, USER);

    UserDataSpecification userDataSpecification =
        UserDataSpecification.builder().data(yaml.getData()).serviceId(serviceId).build();
    userDataSpecification.setAppId(appId);
    return userDataSpecification;
  }

  @Override
  public Class getYamlClass() {
    return UserDataSpecificationYaml.class;
  }

  @Override
  public UserDataSpecification get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId, USER);

    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("Could not lookup service for the yaml file: " + yamlFilePath, serviceId, USER);

    return serviceResourceService.getUserDataSpecification(appId, serviceId);
  }
}
