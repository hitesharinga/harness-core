/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.tasklet;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.health.LastReceivedPublishedMessageDao;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecordDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.CE)
@Slf4j
@Singleton
public class DelegateHealthCheckTasklet implements Tasklet {
  @Autowired private PerpetualTaskRecordDao perpetualTaskRecordDao;
  @Autowired private LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;

  private static final int BATCH_SIZE = 20;
  private static final long DELAY_IN_MINUTES_FOR_LAST_RECEIVED_MSG = 90;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
    final JobConstants jobConstants = CCMJobConstants.fromContext(chunkContext);
    String accountId = jobConstants.getAccountId();
    Instant startTime = Instant.ofEpochMilli(jobConstants.getJobStartTime());
    List<PerpetualTaskRecord> perpetualTasks = perpetualTaskRecordDao.listValidK8sWatchPerpetualTasksForAccount(accountId);
    List<String> clusterIds = new ArrayList<>();
    for (PerpetualTaskRecord perpetualTask: perpetualTasks) {
      if (perpetualTask.getTaskDescription().equals("NG")) {
        String clientId = perpetualTask.getClientContext().getClientId();
        clusterIds.add(clientId.substring(clientId.lastIndexOf('/') + 1));
      } else {
        clusterIds.add(perpetualTask.getClientContext().getClientParams().get("clusterId"));
      }
    }
    Instant allowedTime = startTime.minus(Duration.ofMinutes(DELAY_IN_MINUTES_FOR_LAST_RECEIVED_MSG));
    for (List<String> clusterIdsBatch : Lists.partition(clusterIds, BATCH_SIZE)) {
      Map<String, Long> lastReceivedTimeForClusters =
          lastReceivedPublishedMessageDao.getLastReceivedTimeForClusters(accountId, clusterIdsBatch);
      for (String clusterId: lastReceivedTimeForClusters.keySet()) {
        if (Instant.ofEpochMilli(lastReceivedTimeForClusters.get(clusterId)).isBefore(allowedTime)) {
          log.info("Delegate health check failed for clusterId: {}", clusterId);
        } else {
          log.info("Delegate health check successful for clusterId: {}", clusterId);
        }
      }
    }
    return null;
  }
}
