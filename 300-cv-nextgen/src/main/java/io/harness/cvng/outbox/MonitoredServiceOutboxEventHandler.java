/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.outbox;

import io.harness.audit.client.api.AuditClientService;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.serializer.HObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MonitoredServiceOutboxEventHandler implements OutboxEventHandler {
  private ObjectMapper objectMapper;
  private final AuditClientService auditClientService;

  @Inject
  MonitoredServiceOutboxEventHandler(AuditClientService auditClientService) {
    this.objectMapper = HObjectMapper.NG_DEFAULT_OBJECT_MAPPER;
    this.auditClientService = auditClientService;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    log.info("Outbox event handler: {}", outboxEvent);
    return true;
  }
}
