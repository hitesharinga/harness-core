package io.harness.cvng.statemachine.services.api;

import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.services.api.TrendAnalysisService;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.ServiceGuardTrendAnalysisState;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceGuardTrendAnalysisStateExecutor extends AnalysisStateExecutor<ServiceGuardTrendAnalysisState> {
  @Inject private transient TrendAnalysisService trendAnalysisService;
  private String workerTaskId;

  @Override
  public AnalysisState execute(ServiceGuardTrendAnalysisState analysisState) {
    workerTaskId = trendAnalysisService.scheduleTrendAnalysisTask(analysisState.getInputs());
    analysisState.setStatus(AnalysisStatus.RUNNING);
    log.info("Executing service guard trend analysis for {}", analysisState.getInputs());
    return analysisState;
  }

  @Override
  public AnalysisStatus getExecutionStatus(ServiceGuardTrendAnalysisState analysisState) {
    if (analysisState.getStatus() != AnalysisStatus.SUCCESS) {
      Map<String, LearningEngineTask.ExecutionStatus> taskStatuses =
          trendAnalysisService.getTaskStatus(Collections.singletonList(workerTaskId));
      LearningEngineTask.ExecutionStatus taskStatus = taskStatuses.get(workerTaskId);
      // This could be common code for all states.
      switch (taskStatus) {
        case SUCCESS:
          return AnalysisStatus.SUCCESS;
        case FAILED:
        case TIMEOUT:
          return AnalysisStatus.RETRY;
        case QUEUED:
        case RUNNING:
          return AnalysisStatus.RUNNING;
        default:
          throw new AnalysisStateMachineException(
              "Unknown worker state when executing service guard trend analysis: " + taskStatus);
      }
    }
    return AnalysisStatus.SUCCESS;
  }

  @Override
  public AnalysisState handleRerun(ServiceGuardTrendAnalysisState analysisState) {
    // increment the retryCount without caring for the max
    // clean up state in underlying worker and then execute
    analysisState.setRetryCount(analysisState.getRetryCount() + 1);
    log.info("In service guard trend analysis for Inputs {}, cleaning up worker task. Old taskID: {}",
        analysisState.getInputs(), workerTaskId);
    workerTaskId = null;
    analysisState.execute();
    return analysisState;
  }

  @Override
  public AnalysisState handleRunning(ServiceGuardTrendAnalysisState analysisState) {
    return analysisState;
  }

  @Override
  public AnalysisState handleSuccess(ServiceGuardTrendAnalysisState analysisState) {
    analysisState.setStatus(AnalysisStatus.SUCCESS);
    return analysisState;
  }

  @Override
  public AnalysisState handleTransition(ServiceGuardTrendAnalysisState analysisState) {
    analysisState.setStatus(AnalysisStatus.SUCCESS);
    return analysisState;
  }

  @Override
  public AnalysisState handleRetry(ServiceGuardTrendAnalysisState analysisState) {
    if (analysisState.getRetryCount() >= getMaxRetry()) {
      analysisState.setStatus(AnalysisStatus.FAILED);
    } else {
      analysisState.setRetryCount(analysisState.getRetryCount() + 1);
      log.info("In service guard trend analysis state, for Inputs {}, cleaning up worker task. Old taskID: {}",
          analysisState.getInputs(), workerTaskId);
      workerTaskId = null;
      analysisState.execute();
    }
    return analysisState;
  }
}
