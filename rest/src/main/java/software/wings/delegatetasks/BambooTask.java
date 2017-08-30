package software.wings.delegatetasks;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static software.wings.beans.BambooConfig.Builder.aBambooConfig;
import static software.wings.sm.states.BambooState.BambooExecutionResponse;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.bamboo.Result;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.BambooState;
import software.wings.sm.states.FilePathAssertionEntry;
import software.wings.sm.states.ParameterEntry;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by sgurubelli on 8/29/17.
 */
public class BambooTask extends AbstractDelegateRunnableTask<BambooState.BambooExecutionResponse> {
  private static final Logger logger = LoggerFactory.getLogger(BambooTask.class);

  @Inject private BambooService bambooService;

  public BambooTask(String delegateId, DelegateTask delegateTask, Consumer<BambooExecutionResponse> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public BambooExecutionResponse run(Object[] parameters) {
    BambooExecutionResponse bambooExecutionResponse = new BambooExecutionResponse();
    logger.info("In Bamboo Task run method");
    try {
      bambooExecutionResponse = run((String) parameters[0], (String) parameters[1], (char[]) parameters[2],
          (String) parameters[3], (List<ParameterEntry>) parameters[4], (List<FilePathAssertionEntry>) parameters[5]);
    } catch (Exception e) {
      logger.warn("Failed to execute Bamboo verification task: " + e.getMessage(), e);
      bambooExecutionResponse.setExecutionStatus(ExecutionStatus.FAILED);
    }
    logger.info("Bamboo task  completed");
    return bambooExecutionResponse;
  }

  public BambooExecutionResponse run(String bambooUrl, String username, char[] password, String planKey,
      List<ParameterEntry> parameterEntries, List<FilePathAssertionEntry> filePathAssertionEntries) {
    BambooExecutionResponse bambooExecutionResponse = new BambooExecutionResponse();
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    String errorMessage = null;
    try {
      BambooConfig bambooConfig =
          aBambooConfig().withBambooUrl(bambooUrl).withUsername(username).withPassword(password).build();
      Map<String, String> evaluatedParameters = Maps.newLinkedHashMap();
      if (isNotEmpty(parameterEntries)) {
        parameterEntries.forEach(
            parameterEntry -> { evaluatedParameters.put(parameterEntry.getKey(), parameterEntry.getValue()); });
      }
      String buildResultKey = bambooService.triggerPlan(bambooConfig, planKey, evaluatedParameters);
      // waitForBuildStartExecution(bambooConfig, buildResultKey);
      Result result = waitForBuildExecutionToFinish(bambooConfig, buildResultKey);
      String buildState = result.getBuildState();
      if (result == null || buildState == null) {
        executionStatus = ExecutionStatus.FAILED;
        logger.info("Bamboo execution failed for plan {}", planKey);
      } else {
        if (buildState != null) {
          if (!buildState.equalsIgnoreCase("Successful")) {
            executionStatus = ExecutionStatus.FAILED;
            logger.info("Build result for Bamboo url {}, plan key {}, build key {} is Failed. Result {}", bambooUrl,
                planKey, buildResultKey, result);
          }
        }
        bambooExecutionResponse.setProjectName(result.getProjectName());
        bambooExecutionResponse.setPlanName(result.getPlanName());
        bambooExecutionResponse.setBuildNumber(result.getBuildNumber());
        bambooExecutionResponse.setBuildStatus(result.getBuildState());
        bambooExecutionResponse.setBuildUrl(result.getBuildUrl());
        bambooExecutionResponse.setParameters(parameterEntries);
      }
    } catch (Exception e) {
      logger.warn("Failed to execute Bamboo verification task: " + e.getMessage(), e);
      if (e instanceof WingsException) {
        WingsException ex = (WingsException) e;
        errorMessage = Joiner.on(",").join(ex.getResponseMessageList()
                                               .stream()
                                               .map(responseMessage
                                                   -> ResponseCodeCache.getInstance()
                                                          .getResponseMessage(responseMessage.getCode(), ex.getParams())
                                                          .getMessage())
                                               .collect(toList()));
      } else {
        errorMessage = e.getMessage();
      }
      executionStatus = ExecutionStatus.FAILED;
    }
    bambooExecutionResponse.setErrorMessage(errorMessage);
    bambooExecutionResponse.setExecutionStatus(executionStatus);
    return bambooExecutionResponse;
  }

  private Result waitForBuildExecutionToFinish(BambooConfig bambooConfig, String buildResultKey) throws IOException {
    Result result;
    do {
      logger.info("Waiting for build execution {} to finish", buildResultKey);
      Misc.sleepWithRuntimeException(5000);
      result = bambooService.getBuildResult(bambooConfig, buildResultKey);
      logger.info("Build result for build key {} is {}", buildResultKey, result);
    } while (result.getBuildState() == null || result.getBuildState().equalsIgnoreCase("Unknown"));

    // Get the build result
    logger.info("Build execution for build key {} is finished. Result:{} ", buildResultKey, result);
    return result;
  }

  /*private Status waitForBuildStartExecution(BambooConfig bambooConfig, String buildResultKey) throws IOException {
    Status status;
    do {
      Misc.sleepWithRuntimeException(1000);
      status =  bambooService.getBuildResultStatus(bambooConfig, buildResultKey);
    } while (status == null);
    return status;
  }*/
}
