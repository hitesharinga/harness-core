package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Environment;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceTemplateService;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HelmValuesYamlToManifestFileMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(HelmValuesYamlToManifestFileMigration.class);
  private static final String HELM_VALUE_YAML_KEY = "helmValueYaml";
  private static final String HELM_VALUE_YAML_BY_SERVICE_TEMPLATE_ID_KEY = "helmValueYamlByServiceTemplateId";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private EnvironmentService environmentService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ApplicationManifestService applicationManifestService;

  @Override
  public void migrate() {
    logger.info("Running HelmValuesYamlToManifestFileMigration");

    migrateHelmValuesInEnvironment();

    logger.info("Completed HelmValuesYamlToManifestFileMigration");
  }

  private void migrateHelmValuesInEnvironment() {
    migrateEnvironmentOverrides();
    migrateEnvironmentServiceOverrides();
  }

  private void migrateEnvironmentOverrides() {
    logger.info("Migrating environment overrides");

    List<Environment> environments =
        wingsPersistence.createQuery(Environment.class, excludeAuthority).field(HELM_VALUE_YAML_KEY).exists().asList();

    if (isEmpty(environments)) {
      return;
    }

    for (Environment environment : environments) {
      ApplicationManifest applicationManifest =
          applicationManifestService.getByEnvId(environment.getAppId(), environment.getUuid(), AppManifestKind.VALUES);
      if (applicationManifest == null) {
        ManifestFile manifestFile = ManifestFile.builder().fileContent(environment.getHelmValueYaml()).build();
        environmentService.createValues(environment.getAppId(), environment.getUuid(), null, manifestFile);
      }
    }

    logger.info("Completed migrating environment overrides");
  }

  private void migrateEnvironmentServiceOverrides() {
    logger.info("Migrating environment service overrides");

    List<Environment> environments = wingsPersistence.createQuery(Environment.class, excludeAuthority)
                                         .field(HELM_VALUE_YAML_BY_SERVICE_TEMPLATE_ID_KEY)
                                         .exists()
                                         .asList();

    if (isEmpty(environments)) {
      return;
    }

    for (Environment environment : environments) {
      Map<String, String> helmValueYamlByServiceTemplateId = environment.getHelmValueYamlByServiceTemplateId();

      for (Entry<String, String> entry : helmValueYamlByServiceTemplateId.entrySet()) {
        String serviceTemplateId = entry.getKey();
        String helmValue = entry.getValue();

        ServiceTemplate serviceTemplate = serviceTemplateService.get(environment.getAppId(), serviceTemplateId);
        if (serviceTemplate != null) {
          ApplicationManifest applicationManifest = applicationManifestService.getByEnvAndServiceId(
              environment.getAppId(), environment.getUuid(), serviceTemplate.getServiceId(), AppManifestKind.VALUES);

          if (applicationManifest == null) {
            ManifestFile manifestFile = ManifestFile.builder().fileContent(helmValue).build();
            environmentService.createValues(
                environment.getAppId(), environment.getUuid(), serviceTemplate.getServiceId(), manifestFile);
          }
        }
      }
    }

    logger.info("Completed migrating environment service overrides");
  }
}
