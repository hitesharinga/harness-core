/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.gitintegration.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.idp.gitintegration.entities.CatalogConnectorEntity;
import io.harness.spec.server.idp.v1.model.ConnectorDetails;

import java.util.List;
import java.util.Optional;

@OwnedBy(HarnessTeam.IDP)
public interface GitIntegrationService {
  void createConnectorSecretsEnvVariable(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, ConnectorType connectorType);
  void processConnectorUpdate(Message message, EntityChangeDTO entityChangeDTO);

  void createConnectorInBackstage(String accountIdentifier, String connectorIdentifier, String type);

  List<CatalogConnectorEntity> getAllConnectorDetails(String accountIdentifier);
  Optional<CatalogConnectorEntity> findByAccountIdAndProviderType(String accountIdentifier, String providerType);
  CatalogConnectorEntity saveConnectorDetails(String accountIdentifier, ConnectorDetails connectorDetails);
  CatalogConnectorEntity findDefaultConnectorDetails(String accountIdentifier);
}
