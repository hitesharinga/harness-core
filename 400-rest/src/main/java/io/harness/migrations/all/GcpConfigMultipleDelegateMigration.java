package io.harness.migrations.all;

import static io.fabric8.utils.Strings.isNotBlank;
import static java.lang.String.format;
import static org.reflections.Reflections.log;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.GcpConfig;
import software.wings.beans.GcpConfig.GcpConfigKeys;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

@TargetModule(Module._390_DB_MIGRATION)
public class GcpConfigMultipleDelegateMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;

  private final String DEBUG_LINE = "GCP_CONFIG_MULTIPLE_DELEGATE_MIGRATION: ";

  @Override
  public void migrate() {
    log.info("Running GcpConfigMultipleDelegateMigration");
    List<Account> allAccounts = accountService.listAllAccountWithDefaultsWithoutLicenseInfo();

    for (Account account : allAccounts) {
      String accountId = account.getUuid();
      log.info(StringUtils.join(DEBUG_LINE, "Starting moving variable names for accountId:", accountId));

      try (HIterator<SettingAttribute> settingAttributes =
               new HIterator<>(wingsPersistence.createQuery(SettingAttribute.class)
                                   .filter(SettingAttribute.SettingAttributeKeys.accountId, accountId)
                                   .fetch())) {
        while (settingAttributes.hasNext()) {
          SettingAttribute settingAttribute = settingAttributes.next();

          try {
            if (settingAttribute.getValue() instanceof GcpConfig) {
              GcpConfig clusterConfig = (GcpConfig) settingAttribute.getValue();
              boolean useDelegateSelectors = clusterConfig.isUseDelegate();
              wingsPersistence.updateField(SettingAttribute.class, settingAttribute.getUuid(),
                  new StringBuilder()
                      .append(SettingAttributeKeys.value)
                      .append(".")
                      .append(GcpConfigKeys.useDelegateSelectors)
                      .toString(),
                  useDelegateSelectors);
              log.info(StringUtils.join(DEBUG_LINE,
                  format("useDelegate to useDelegateSelectors migration done for settingAttribute for %s",
                      settingAttribute.getUuid())));
              if ((clusterConfig.isUseDelegate() || clusterConfig.isUseDelegateSelectors())
                  && isNotBlank(clusterConfig.getDelegateSelector())) {
                List<String> delegateSelectors = Collections.singletonList(clusterConfig.getDelegateSelector());
                wingsPersistence.updateField(SettingAttribute.class, settingAttribute.getUuid(),
                    new StringBuilder()
                        .append(SettingAttributeKeys.value)
                        .append(".")
                        .append(GcpConfigKeys.delegateSelectors)
                        .toString(),
                    delegateSelectors);
                log.info(StringUtils.join(DEBUG_LINE,
                    format("DelegateSelector to DelegateSelectors migration done for settingAttribute for %s",
                        settingAttribute.getUuid())));
              } else {
                log.info(StringUtils.join(DEBUG_LINE,
                    format(
                        "isUseDelegate and isUseDelegateSelectors fields are false for settingAttribute %s, skipping",
                        settingAttribute.getUuid())));
              }
            } else {
              log.info(StringUtils.join(DEBUG_LINE,
                  format("setting value is not of type GcpConfig for %s , skipping", settingAttribute.getUuid())));
            }
          } catch (Exception ex) {
            log.error(StringUtils.join(DEBUG_LINE,
                format("Error  moving variable names for settingAttribute %s ", settingAttribute.getUuid()), ex));
          }
        }
      } catch (Exception ex) {
        log.error(StringUtils.join(DEBUG_LINE, format("Error  moving variable names for accountId %s", accountId), ex));
      }
      log.info(StringUtils.join(DEBUG_LINE, format("Successfully moved variable names for accountId %s", accountId)));
    }
    log.info("Completed GcpConfigMultipleDelegateMigration");
  }
}
