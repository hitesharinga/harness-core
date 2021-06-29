package io.harness.ccm.service.impl;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.ccm.commons.beans.config.CEFeatures;
import io.harness.ccm.remote.beans.K8sClusterSetupRequest;
import io.harness.ccm.service.intf.CEYamlService;
import io.harness.delegate.beans.connector.k8Connector.K8sServiceAccountInfoResponse;
import io.harness.exception.UnexpectedException;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.hazelcast.util.Preconditions;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
public class CEYamlServiceImpl implements CEYamlService {
  private static final Configuration templateConfiguration = new Configuration(VERSION_2_3_23);
  private static final String YAML_FTL = ".yaml.ftl";

  @Inject private K8sServiceAccountDelegateTaskClient k8sTaskClient;

  static {
    templateConfiguration.setTemplateLoader(new ClassTemplateLoader(CEYamlServiceImpl.class, "/yamltemplates"));
  }

  @Override
  @Deprecated
  public File downloadCostOptimisationYaml(String accountId, String connectorIdentifier, String apiKey,
      String harnessHost, String serverName) throws IOException {
    final String costOptimisationFileName = "cost-optimisation-crd";

    ImmutableMap<String, String> scriptParams = ImmutableMap.<String, String>builder()
                                                    .put("accountId", accountId)
                                                    .put("connectorIdentifier", connectorIdentifier)
                                                    .put("envoyHarnessHostname", serverName)
                                                    .put("harnessHostname", harnessHost)
                                                    .put("APIToken", apiKey)
                                                    .build();

    File yaml = File.createTempFile(costOptimisationFileName, DOT_YAML);

    saveProcessedTemplate(scriptParams, yaml, costOptimisationFileName + YAML_FTL);
    return new File(yaml.getAbsolutePath());
  }

  @Override
  public String unifiedCloudCostK8sClusterYaml(@NonNull String accountId, String harnessHost, String serverName,
      @NonNull K8sClusterSetupRequest request) throws IOException {
    String yamlFileContent = "";

    if (request.getFeaturesEnabled().contains(CEFeatures.VISIBILITY)) {
      yamlFileContent = getK8sVisibilityYaml(
          accountId, request.getConnectorIdentifier(), request.getOrgIdentifier(), request.getProjectIdentifier());
    }

    if (request.getFeaturesEnabled().contains(CEFeatures.OPTIMIZATION)) {
      if (!yamlFileContent.isEmpty()) {
        yamlFileContent += "\n---\n";
      }

      yamlFileContent += getK8sOptimisationYaml(
          accountId, request.getConnectorIdentifier(), request.getApiKey(), harnessHost, serverName);
    }

    return yamlFileContent;
  }

  private String getK8sOptimisationYaml(@NonNull String accountId, @NonNull String connectorIdentifier,
      @NonNull String apiKey, @NonNull String harnessHost, @NonNull String serverName) throws IOException {
    final String costOptimisationFileName = "cost-optimisation-crd";

    ImmutableMap<String, String> scriptParams = ImmutableMap.<String, String>builder()
                                                    .put("accountId", accountId)
                                                    .put("connectorIdentifier", connectorIdentifier)
                                                    .put("envoyHarnessHostname", serverName)
                                                    .put("harnessHostname", harnessHost)
                                                    .put("APIToken", apiKey)
                                                    .build();

    return getProcessedYaml(costOptimisationFileName, scriptParams);
  }

  private String getK8sVisibilityYaml(@NonNull String accountId, @NonNull String connectorIdentifier,
      String orgIdentifier, String projectIdentifier) throws IOException {
    final String visibilityYamlFileName = "k8s-visibility-clusterrole";
    K8sServiceAccountInfoResponse serviceAccount;
    try {
      serviceAccount =
          k8sTaskClient.fetchServiceAccount(connectorIdentifier, accountId, orgIdentifier, projectIdentifier);
    } catch (Exception ex) {
      log.error("Failed delegate task K8S_SERVICE_ACCOUNT_INFO", ex);
      throw ex;
    }

    log.info(
        "serviceAccount associated with accountId:{}, connectorIdentifier:{}, orgIdentifier:{}, projectIdentifier:{} is {}",
        accountId, connectorIdentifier, orgIdentifier, projectIdentifier, serviceAccount);

    // "username": "system:serviceaccount:harness-delegate:default"
    final String[] username = serviceAccount.getUsername().split(":");

    Preconditions.checkState(username.length == 4,
        String.format("serviceAccount username [%s] is not of size 4", serviceAccount.getUsername()));
    final String namespace = username[2];
    final String name = username[3];

    ImmutableMap<String, String> scriptParams = ImmutableMap.<String, String>builder()
                                                    .put("serviceAccountName", name)
                                                    .put("serviceAccountNamespace", namespace)
                                                    .build();

    return getProcessedYaml(visibilityYamlFileName, scriptParams);
  }

  private String getProcessedYaml(@NonNull String yamlFileName, @NonNull ImmutableMap<String, String> params)
      throws IOException {
    File file = File.createTempFile(yamlFileName, DOT_YAML);
    saveProcessedTemplate(params, file, yamlFileName + YAML_FTL);

    final String yamlFileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    file.delete();
    return yamlFileContent;
  }

  private void saveProcessedTemplate(Map<String, String> params, File start, String template) throws IOException {
    try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(start), UTF_8)) {
      templateConfiguration.getTemplate(template).process(params, fileWriter);
    } catch (TemplateException ex) {
      throw new UnexpectedException("This templates are included in the jar, they should be safe to process", ex);
    }
  }
}
