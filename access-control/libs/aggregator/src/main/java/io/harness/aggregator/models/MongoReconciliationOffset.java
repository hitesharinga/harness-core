/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aggregator.models;

import static io.harness.aggregator.models.MongoReconciliationOffset.PRIMARY_COLLECTION;
import static io.harness.ng.DbAliases.ACCESS_CONTROL;

import io.harness.annotations.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;

import dev.morphia.annotations.Entity;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
@StoreIn(ACCESS_CONTROL)
@Document(PRIMARY_COLLECTION)
@Entity(value = PRIMARY_COLLECTION, noClassnameStored = true)
@FieldNameConstants(innerTypeName = "keys")
@TypeAlias(PRIMARY_COLLECTION)
public class MongoReconciliationOffset implements PersistentEntity {
  public static final String PRIMARY_COLLECTION = "mongoReconciliationOffset";
  public static final String SECONDARY_COLLECTION = "mongoReconciliationOffset_secondary";

  @Id @dev.morphia.annotations.Id private String id;
  private byte[] key;
  private byte[] value;
  @FdIndex @CreatedDate private long createdAt;
}
