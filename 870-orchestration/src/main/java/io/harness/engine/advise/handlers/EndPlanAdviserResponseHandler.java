package io.harness.engine.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.EndPlanAdvise;
import io.harness.pms.contracts.interrupts.InterruptType;

import com.google.inject.Inject;

@OwnedBy(CDC)
public class EndPlanAdviserResponseHandler implements AdviserResponseHandler {
  @Inject private OrchestrationEngine engine;
  @Inject private InterruptManager interruptManager;

  @Override
  public void handleAdvise(NodeExecution nodeExecution, AdviserResponse adviserResponse) {
    EndPlanAdvise endPlanAdvise = adviserResponse.getEndPlanAdvise();
    if (endPlanAdvise != null && endPlanAdvise.getIsAbort()) {
      InterruptPackage interruptPackage = InterruptPackage.builder()
                                              .planExecutionId(nodeExecution.getAmbiance().getPlanExecutionId())
                                              .nodeExecutionId(nodeExecution.getUuid())
                                              .interruptType(InterruptType.ABORT_ALL)
                                              .build();
      interruptManager.register(interruptPackage);
    } else {
      engine.endTransition(nodeExecution, adviserResponse);
    }
  }
}
