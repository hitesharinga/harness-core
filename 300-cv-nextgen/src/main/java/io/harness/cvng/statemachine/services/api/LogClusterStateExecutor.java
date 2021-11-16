package io.harness.cvng.statemachine.services.api;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.services.api.LogClusterService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.LogClusterState;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class LogClusterStateExecutor<T extends LogClusterState> extends AnalysisStateExecutor<T> {
  protected LogClusterLevel clusterLevel;
  @Inject protected transient LogClusterService logClusterService;
  private Set<String> workerTaskIds;
  private Map<String, LearningEngineTask.ExecutionStatus> workerTaskStatus;

  protected abstract List<String> scheduleAnalysis(AnalysisInput analysisInput);

  @Override
  public AnalysisState execute(T analysisState) {
    List<String> taskIds = scheduleAnalysis(analysisState.getInputs());
    if (isNotEmpty(taskIds)) {
      if (workerTaskIds == null) {
        workerTaskIds = new HashSet<>();
      }
      workerTaskIds.addAll(taskIds);
      analysisState.setStatus(AnalysisStatus.RUNNING);
      log.info("Executing ServiceGuardLogClusterState for input: {}. Created {} tasks", analysisState.getInputs(),
          workerTaskIds.size());
    } else {
      log.error(
          "Executing ServiceGuardLogClusterState for input: {}. No clustering tasks were created. This is an error state",
          analysisState.getInputs());
      throw new IllegalStateException("LogClusterState for input: " + analysisState.getInputs()
          + ". No clustering tasks were created."
          + " This is an error state");
    }
    return analysisState;
  }

  @Override
  public AnalysisStatus getExecutionStatus(T analysisState) {
    if (!analysisState.getStatus().equals(AnalysisStatus.SUCCESS)) {
      Map<String, LearningEngineTask.ExecutionStatus> taskStatuses = logClusterService.getTaskStatus(workerTaskIds);
      Map<LearningEngineTask.ExecutionStatus, Set<String>> statusTaskMap = new HashMap<>();
      taskStatuses.forEach((taskId, taskStatus) -> {
        if (!statusTaskMap.containsKey(taskStatus)) {
          statusTaskMap.put(taskStatus, new HashSet<>());
        }
        statusTaskMap.get(taskStatus).add(taskId);
      });

      log.info("Current statuses of worker tasks with inputs {} is {}", analysisState.getInputs(), statusTaskMap);
      if (statusTaskMap.containsKey(LearningEngineTask.ExecutionStatus.SUCCESS)
          && workerTaskIds.size() == statusTaskMap.get(LearningEngineTask.ExecutionStatus.SUCCESS).size()) {
        log.info("All worker tasks have succeeded.");
        return AnalysisStatus.TRANSITION;
      } else {
        if (statusTaskMap.containsKey(LearningEngineTask.ExecutionStatus.RUNNING)
            || statusTaskMap.containsKey(LearningEngineTask.ExecutionStatus.QUEUED)) {
          return AnalysisStatus.RUNNING;
        }
      }
    }
    return AnalysisStatus.TRANSITION;
  }

  @Override
  public AnalysisState handleRerun(T analysisState) {
    // TODO: To be implemented
    return null;
  }

  @Override
  public AnalysisState handleRunning(T analysisState) {
    return analysisState;
  }

  @Override
  public AnalysisState handleSuccess(T analysisState) {
    analysisState.setStatus(AnalysisStatus.SUCCESS);
    return analysisState;
  }

  @Override
  public AnalysisState handleRetry(T analysisState) {
    // TODO: To be implemented
    return null;
  }
}
