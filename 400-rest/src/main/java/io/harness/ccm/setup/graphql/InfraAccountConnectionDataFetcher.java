package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.ccm.setup.util.InfraSetUpUtils;

import software.wings.app.MainConfiguration;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.security.authentication.SimpleUrlBuilder;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CE)
public class InfraAccountConnectionDataFetcher
    extends AbstractObjectDataFetcher<QLInfraAccountConnectionData, QLInfraType> {
  @Inject private MainConfiguration mainConfiguration;

  private static final String stackBaseUrl = "https://console.aws.amazon.com/cloudformation/home?#/stacks/quickcreate";
  private static final String stackSetBaseUrl = "https://console.aws.amazon.com/cloudformation/home?#/stacksets/create";
  private static final String stackNameKey = "stackName";
  private static final String templateUrlKey = "templateURL";
  private static final String paramExternalIdKey = "param_ExternalId";
  private static final String stackNameValue = "harness-ce-iam-role-stack";
  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLInfraAccountConnectionData fetch(QLInfraType parameters, String accountId) {
    CESetUpConfig ceSetUpConfig = mainConfiguration.getCeSetUpConfig();

    String awsAccountId = InfraSetUpUtils.getCEAwsAccountId(ceSetUpConfig.getAwsAccountId());
    if (parameters.getInfraType() == QLInfraTypesEnum.AWS) {
      String awsExternalId = InfraSetUpUtils.getAwsExternalId(awsAccountId, accountId);
      String masterLaunchTemplateUrl =
          new SimpleUrlBuilder(stackBaseUrl)
              .addQueryParam(stackNameKey, stackNameValue)
              .addQueryParam(templateUrlKey, ceSetUpConfig.getMasterAccountCloudFormationTemplateLink())
              .addQueryParam(paramExternalIdKey, awsExternalId)
              .build();

      return QLInfraAccountConnectionData.builder()
          .externalId(awsExternalId)
          .harnessAccountId(awsAccountId)
          .masterAccountCloudFormationTemplateLink(ceSetUpConfig.getMasterAccountCloudFormationTemplateLink())
          .linkedAccountCloudFormationTemplateLink(ceSetUpConfig.getLinkedAccountCloudFormationTemplateLink())
          .masterAccountLaunchTemplateLink(masterLaunchTemplateUrl)
          .linkedAccountLaunchTemplateLink(stackSetBaseUrl)
          .build();
    }
    if (parameters.getInfraType() == QLInfraTypesEnum.AZURE) {
      return QLInfraAccountConnectionData.builder()
          .azureHarnessAppClientId(mainConfiguration.getCeSetUpConfig().getAzureAppClientId())
          .build();
    }
    return QLInfraAccountConnectionData.builder().build();
  }
}
