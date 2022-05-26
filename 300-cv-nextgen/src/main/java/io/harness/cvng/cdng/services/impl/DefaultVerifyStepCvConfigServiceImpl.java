/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.services.impl;

import io.harness.cvng.cdng.beans.MonitoredServiceNode;
import io.harness.cvng.cdng.services.api.VerifyStepCvConfigService;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.collections.CollectionUtils;

public class DefaultVerifyStepCvConfigServiceImpl implements VerifyStepCvConfigService {
  @Inject private CVConfigService cvConfigService;
  @Inject private MonitoredServiceService monitoredServiceService;

  @Override
  public String getMonitoredServiceIdentifier(
      ServiceEnvironmentParams serviceEnvironmentParams, MonitoredServiceNode monitoredServiceNode) {
    MonitoredService monitoredService = getMonitoredService(serviceEnvironmentParams);
    return monitoredService.getIdentifier();
  }

  @Override
  public List<CVConfig> getCVConfigs(
      ServiceEnvironmentParams serviceEnvironmentParams, MonitoredServiceNode monitoredServiceNode) {
    MonitoredService monitoredService = getMonitoredService(serviceEnvironmentParams);
    if (Objects.nonNull(monitoredService)
        && CollectionUtils.isNotEmpty(monitoredService.getHealthSourceIdentifiers())) {
      return getCvConfigsFromMonitoredService(serviceEnvironmentParams, monitoredService.getIdentifier());
    } else {
      return Collections.emptyList();
    }
  }

  private List<CVConfig> getCvConfigsFromMonitoredService(
      ServiceEnvironmentParams serviceEnvironmentParams, String monitoredServiceIdentifier) {
    return cvConfigService.listByMonitoringSources(serviceEnvironmentParams.getAccountIdentifier(),
        serviceEnvironmentParams.getOrgIdentifier(), serviceEnvironmentParams.getProjectIdentifier(),
        Collections.singletonList(monitoredServiceIdentifier));
  }

  private MonitoredService getMonitoredService(ServiceEnvironmentParams serviceEnvironmentParams) {
    return monitoredServiceService.getApplicationMonitoredService(serviceEnvironmentParams).orElse(null);
  }
}
