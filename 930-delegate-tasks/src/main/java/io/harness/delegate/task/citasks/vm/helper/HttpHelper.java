/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.citasks.vm.helper;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.citasks.vm.helper.CIVMConstants.RUNNER_CONNECT_TIMEOUT_SECS;
import static io.harness.delegate.task.citasks.vm.helper.CIVMConstants.RUNNER_URL;
import static io.harness.delegate.task.citasks.vm.helper.CIVMConstants.RUNNER_URL_ENV;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.vm.runner.DestroyVmRequest;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepRequest;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepResponse;
import io.harness.delegate.beans.ci.vm.runner.PoolOwnerStepResponse;
import io.harness.delegate.beans.ci.vm.runner.SetupVmRequest;
import io.harness.delegate.beans.ci.vm.runner.SetupVmResponse;
import io.harness.exception.PodNotFoundException;
import io.harness.network.Http;
import io.harness.threading.Sleeper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.MediaType;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CI)
public class HttpHelper {
  public static final MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");
  private final int MAX_ATTEMPTS = 3;
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(2);
  private final int DELETION_MAX_ATTEMPTS = 15;
  @Inject private Sleeper sleeper;
  public RunnerRestClient getRunnerClient(int timeoutInSecs) {
    String runnerUrl = getRunnerUrl();
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(runnerUrl)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .client(Http.getUnsafeOkHttpClient(runnerUrl, RUNNER_CONNECT_TIMEOUT_SECS, timeoutInSecs))
                            .build();
    return retrofit.create(RunnerRestClient.class);
  }

  private String getRunnerUrl() {
    String url = System.getenv(RUNNER_URL_ENV);
    if (isNotEmpty(url)) {
      return url;
    }
    return RUNNER_URL;
  }

  public Response<SetupVmResponse> setupStageWithRetries(SetupVmRequest setupVmRequest) {
    // TODO(shubham): Retry on stage setup can create 2 VMs.
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying failed to setup stage; attempt: {}", "Failing to setup stage after retrying {} times");
    return Failsafe.with(retryPolicy).get(() -> getRunnerClient(600).setup(setupVmRequest).execute());
  }

  public Response<ExecuteStepResponse> executeStepWithRetries(ExecuteStepRequest executeStepRequest) {
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        "[Retrying failed to execute step; attempt: {}", "Failing to execute step after retrying {} times");
    return Failsafe.with(retryPolicy).get(() -> getRunnerClient(14400).step(executeStepRequest).execute());
  }

  public Response<Void> cleanupStageWithRetries(DestroyVmRequest destroyVmRequest) {
    RetryPolicy<Object> retryPolicy = getRetryPolicyForDeletion(
        "[Retrying failed to cleanup stage; attempt: {}", "Failing to cleanup stage after retrying {} times");
    return Failsafe.with(retryPolicy).get(() -> cleanupStage(destroyVmRequest));
  }

  public Response<Void> cleanupStage(DestroyVmRequest destroyVmRequest) throws IOException {
    Response<Void> response = getRunnerClient(600).destroy(destroyVmRequest).execute();

    if (response.isSuccessful()) {
      return response;
    }
    throw new RuntimeException(format("Failed to delete VM with stage runtime ID: %s, pool Id: %s",
        destroyVmRequest.getId(), destroyVmRequest.getPoolID()));
  }

  public Response<PoolOwnerStepResponse> isPoolOwner(String poolId) {
    RetryPolicy<Object> retryPolicy = getRetryPolicy("[Retrying failed to check for pool_owner; attempt: {}",
        "Failing to check for pool_owner after retrying {} times");
    return Failsafe.with(retryPolicy).get(() -> getRunnerClient(30).poolOwner(poolId, null).execute());
  }

  public boolean isPoolOwnerWithStageId(String poolId, String stageId) {
    Instant startTime = Instant.now();
    Instant currTime = startTime;
    int maxWaitTime = 60;
    int count = 1;
    while (Duration.between(startTime, currTime).getSeconds() < maxWaitTime) {
      try {
        Response<PoolOwnerStepResponse> response = getRunnerClient(2).poolOwner(poolId, stageId).execute();
        if (response.isSuccessful() && response.body().isOwner()) {
          return true;
        } else {
          log.info("Failing to check for pool_owner after retrying {} times", count);
        }
      } catch (IOException ex) {
        log.info("Failed to find pool owner after retrying {} times: {}", count, ex.getMessage());
      }

      try {
        sleeper.sleep(RETRY_SLEEP_DURATION.toMillis());
      } catch (InterruptedException ex) {
        log.warn("failed to sleep on pool owner call", ex);
      }
      currTime = Instant.now();
      count++;
    }
    return false;
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }

  private RetryPolicy<Object> getRetryPolicyForDeletion(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withMaxAttempts(DELETION_MAX_ATTEMPTS)
        .withBackoff(5, 60, ChronoUnit.SECONDS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
