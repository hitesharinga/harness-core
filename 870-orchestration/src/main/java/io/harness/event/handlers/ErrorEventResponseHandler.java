package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.EventErrorRequest;
import io.harness.pms.execution.SdkResponseEventInternal;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.tasks.FailureResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class ErrorEventResponseHandler implements SdkResponseEventHandler {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public void handleEvent(SdkResponseEventInternal event) {
    EventErrorRequest request = event.getSdkResponseEventRequest().getEventErrorRequest();
    waitNotifyEngine.doneWith(request.getEventNotifyId(),
        FailureResponseData.builder()
            .errorMessage(request.getFailureInfo().getErrorMessage())
            .failureTypes(
                EngineExceptionUtils.transformToWingsFailureTypes(request.getFailureInfo().getFailureTypesList()))
            .build());
  }
}
