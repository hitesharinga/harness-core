/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SumoConfig;
import software.wings.beans.SumoConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class SumoConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, SumoConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SumoConfig config = (SumoConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .type(config.getType())
                    .harnessApiVersion(getHarnessApiVersion())
                    .sumoUrl(config.getSumoUrl())
                    .accessId(getEncryptedYamlRef(config.getAccountId(), config.getEncryptedAccessId()))
                    .accessKey(getEncryptedYamlRef(config.getAccountId(), config.getEncryptedAccessKey()))
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SumoConfig config = new SumoConfig();
    config.setAccountId(accountId);
    config.setSumoUrl(yaml.getSumoUrl());
    config.setEncryptedAccessId(yaml.getAccessId());
    config.setEncryptedAccessKey(yaml.getAccessKey());

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
