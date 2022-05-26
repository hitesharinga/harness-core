/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.environment.services.impl;

import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Producer;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.ng.core.environment.beans.NGServiceOverridesEntity;
import io.harness.ng.core.environment.beans.NGServiceOverridesEntity.NGServiceOverridesEntityKeys;
import io.harness.ng.core.environment.services.ServiceOverrideService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.environment.spring.ServiceOverrideRepository;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(HarnessTeam.CDC)
@Singleton
@Slf4j
public class ServiceOverrideServiceImpl implements ServiceOverrideService {
  private final ServiceOverrideRepository serviceOverrideRepository;
  private final EntitySetupUsageService entitySetupUsageService;
  private final Producer eventProducer;
  private final OutboxService outboxService;
  private final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_TRANSACTION_RETRY_POLICY;
  @Inject @Named(OUTBOX_TRANSACTION_TEMPLATE) private final TransactionTemplate transactionTemplate;

  @Inject
  public ServiceOverrideServiceImpl(ServiceOverrideRepository serviceOverrideRepository,
      EntitySetupUsageService entitySetupUsageService, @Named(ENTITY_CRUD) Producer eventProducer,
      OutboxService outboxService, TransactionTemplate transactionTemplate) {
    this.serviceOverrideRepository = serviceOverrideRepository;
    this.entitySetupUsageService = entitySetupUsageService;
    this.eventProducer = eventProducer;
    this.outboxService = outboxService;
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public Optional<NGServiceOverridesEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String environmentRef, String serviceRef) {
    return serviceOverrideRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndEnvironmentRefAndServiceRef(
        accountId, orgIdentifier, projectIdentifier, environmentRef, serviceRef);
  }

  @Override
  public NGServiceOverridesEntity upsert(NGServiceOverridesEntity requestServiceOverride) {
    validatePresenceOfRequiredFields(requestServiceOverride.getAccountId(), requestServiceOverride.getOrgIdentifier(),
        requestServiceOverride.getProjectIdentifier(), requestServiceOverride.getEnvironmentRef(),
        requestServiceOverride.getServiceRef());
    Criteria criteria = getServiceOverrideEqualityCriteria(requestServiceOverride);
    NGServiceOverridesEntity updatedResult =
        Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
          NGServiceOverridesEntity tempResult = serviceOverrideRepository.upsert(criteria, requestServiceOverride);
          if (tempResult == null) {
            throw new InvalidRequestException(String.format(
                "NGServiceOverridesEntity under Project[%s], Organization [%s], Environment [%s] and Service [%s] couldn't be upserted or doesn't exist.",
                requestServiceOverride.getProjectIdentifier(), requestServiceOverride.getOrgIdentifier(),
                requestServiceOverride.getEnvironmentRef(), requestServiceOverride.getServiceRef()));
          }
          // todo: events for outbox service
          return tempResult;
        }));

    return updatedResult;
  }

  @Override
  public Page<NGServiceOverridesEntity> list(Criteria criteria, Pageable pageable) {
    return serviceOverrideRepository.findAll(criteria, pageable);
  }

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  private Criteria getServiceOverrideEqualityCriteria(NGServiceOverridesEntity requestServiceOverride) {
    return Criteria.where(NGServiceOverridesEntityKeys.accountId)
        .is(requestServiceOverride.getAccountId())
        .and(NGServiceOverridesEntityKeys.orgIdentifier)
        .is(requestServiceOverride.getOrgIdentifier())
        .and(NGServiceOverridesEntityKeys.projectIdentifier)
        .is(requestServiceOverride.getProjectIdentifier())
        .and(NGServiceOverridesEntityKeys.environmentRef)
        .is(requestServiceOverride.getEnvironmentRef())
        .and(NGServiceOverridesEntityKeys.serviceRef)
        .is(requestServiceOverride.getServiceRef());
  }
}
