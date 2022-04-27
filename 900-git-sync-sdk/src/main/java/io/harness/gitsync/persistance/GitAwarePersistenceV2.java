/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.persistance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitaware.dto.GitContextRequestParams;
import io.harness.gitsync.beans.StoreType;
import io.harness.persistence.gitaware.GitAware;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PL)
public interface GitAwarePersistenceV2 {
  <B extends GitAware> Optional<B> findOne(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      Class<B> entityClass, Criteria criteria);

  <B extends GitAware> B save(B objectToSave, String yaml, ChangeType changeType, Class<B> entityClass,
      Supplier functor, StoreType storeType, GitContextRequestParams gitContextRequestParams);
}
