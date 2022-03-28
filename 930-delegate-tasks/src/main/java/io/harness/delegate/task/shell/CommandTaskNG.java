/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.task.shell.SshSessionConfigMapper;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.utils.SshUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.shell.AbstractScriptExecutor;
import io.harness.shell.ExecuteCommandResponse;
import io.harness.shell.ScriptProcessExecutor;
import io.harness.shell.ScriptSshExecutor;
import io.harness.shell.ShellExecutorConfig;
import io.harness.shell.SshSessionConfig;
import io.harness.shell.SshSessionManager;
import io.harness.ssh.SshCommandUnitConstants;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
@OwnedBy(CDP)
public class CommandTaskNG extends AbstractDelegateRunnableTask {
  @Inject private SshExecutorFactoryNG sshExecutorFactoryNG;
  @Inject private ShellExecutorFactoryNG shellExecutorFactory;
  @Inject private SshSessionConfigMapper sshSessionConfigMapper;
  @Inject private SshInitCommandHandler sshInitCommandHandler;
  @Inject private SshCleanupCommandHandler sshCleanupCommandHandler;

  public CommandTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    if (parameters instanceof SshCommandTaskParameters) {
      return runSsh((SshCommandTaskParameters) parameters);
    } else if (parameters instanceof WinrmTaskParameters) {
      // TODO winrm logic
      return CommandTaskResponse.builder().status(CommandExecutionStatus.SUCCESS).build();
    } else {
      throw new IllegalArgumentException(String.format("Invalid parameters type provide %s", parameters.getClass()));
    }
  }

  private DelegateResponseData runSsh(SshCommandTaskParameters parameters) {
    CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

    Function<String, AbstractScriptExecutor> getExecutorFun =
        commandUnit -> getExecutor(parameters, commandUnit, commandUnitsProgress);

    try {
      AbstractScriptExecutor initExecutor = getExecutorFun.apply(SshCommandUnitConstants.Init);
      String script = sshInitCommandHandler.prepareScript(parameters, initExecutor);

      AbstractScriptExecutor executor = getExecutorFun.apply(SshCommandUnitConstants.Exec);
      ExecuteCommandResponse executeCommandResponse = executor.executeCommandString(script, Collections.emptyList());

      // if cleanup fails then the execution command should not fail
      try {
        AbstractScriptExecutor cleanupExecutor = getExecutorFun.apply(SshCommandUnitConstants.Cleanup);
        sshCleanupCommandHandler.cleanup(parameters, cleanupExecutor);
      } catch (Exception e) {
        log.error("Failed to cleanup ssh", e);
      }

      return CommandTaskResponse.builder()
          .executeCommandResponse(executeCommandResponse)
          .status(executeCommandResponse.getStatus())
          .errorMessage(getErrorMessage(executeCommandResponse.getStatus()))
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .build();

    } catch (Exception e) {
      log.error("Bash Script Failed to execute.", e);
      return ShellScriptTaskResponseNG.builder()
          .status(CommandExecutionStatus.FAILURE)
          .errorMessage("Bash Script Failed to execute. Reason: " + e.getMessage())
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .build();
    } finally {
      if (!parameters.executeOnDelegate) {
        SshSessionManager.evictAndDisconnectCachedSession(parameters.getExecutionId(), parameters.getHost());
      }
    }
  }

  private ScriptSshExecutor getScriptSshExecutor(
      SshCommandTaskParameters parameters, String commandUnit, CommandUnitsProgress commandUnitsProgress) {
    SshSessionConfig sshSessionConfig =
        SshUtils.generateSshSessionConfig(sshSessionConfigMapper, parameters, commandUnit);
    return sshExecutorFactoryNG.getExecutor(sshSessionConfig, this.getLogStreamingTaskClient(), commandUnitsProgress);
  }

  private ScriptProcessExecutor getScriptProcessExecutor(
      SshCommandTaskParameters parameters, String commandUnit, CommandUnitsProgress commandUnitsProgress) {
    ShellExecutorConfig config = getShellExecutorConfig(parameters, commandUnit);
    return shellExecutorFactory.getExecutor(config, this.getLogStreamingTaskClient(), commandUnitsProgress);
  }

  private AbstractScriptExecutor getExecutor(
      SshCommandTaskParameters parameters, String commandUnit, CommandUnitsProgress commandUnitsProgress) {
    return parameters.executeOnDelegate ? getScriptProcessExecutor(parameters, commandUnit, commandUnitsProgress)
                                        : getScriptSshExecutor(parameters, commandUnit, commandUnitsProgress);
  }

  private ShellExecutorConfig getShellExecutorConfig(SshCommandTaskParameters taskParameters, String commandUnit) {
    return ShellExecutorConfig.builder()
        .accountId(taskParameters.getAccountId())
        .executionId(taskParameters.getExecutionId())
        .commandUnitName(commandUnit)
        .workingDirectory(taskParameters.getWorkingDirectory())
        .environment(taskParameters.getEnvironmentVariables())
        .scriptType(taskParameters.getScriptType())
        .build();
  }

  private String getErrorMessage(CommandExecutionStatus status) {
    switch (status) {
      case QUEUED:
        return "Shell Script execution queued.";
      case FAILURE:
        return "Shell Script execution failed. Please check execution logs.";
      case RUNNING:
        return "Shell Script execution running.";
      case SKIPPED:
        return "Shell Script execution skipped.";
      case SUCCESS:
      default:
        return "";
    }
  }
}
