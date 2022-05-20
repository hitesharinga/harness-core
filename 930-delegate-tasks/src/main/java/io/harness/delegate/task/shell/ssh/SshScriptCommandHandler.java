/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.shell.ssh;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.task.shell.SshSessionConfigMapper;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.shell.CommandTaskParameters;
import io.harness.delegate.task.shell.ShellExecutorFactoryNG;
import io.harness.delegate.task.shell.SshCommandTaskParameters;
import io.harness.delegate.task.shell.SshExecutorFactoryNG;
import io.harness.delegate.task.ssh.NgCommandUnit;
import io.harness.delegate.task.ssh.ScriptCommandUnit;
import io.harness.delegate.utils.SshUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.shell.AbstractScriptExecutor;
import io.harness.shell.ScriptProcessExecutor;
import io.harness.shell.ScriptSshExecutor;
import io.harness.shell.ScriptType;
import io.harness.shell.ShellExecutorConfig;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class SshScriptCommandHandler implements CommandHandler {
  @Inject private SshExecutorFactoryNG sshExecutorFactoryNG;
  @Inject private ShellExecutorFactoryNG shellExecutorFactory;
  @Inject private SshSessionConfigMapper sshSessionConfigMapper;

  @Override
  public CommandExecutionStatus handle(CommandTaskParameters parameters, NgCommandUnit commandUnit,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    if (!(parameters instanceof SshCommandTaskParameters)) {
      throw new InvalidRequestException("Invalid task parameters submitted for command task.");
    }
    SshCommandTaskParameters sshCommandTaskParameters = (SshCommandTaskParameters) parameters;

    if (!(commandUnit instanceof ScriptCommandUnit)) {
      throw new InvalidRequestException("Bad command unit handler specified.");
    }
    ScriptCommandUnit scriptCommandUnit = (ScriptCommandUnit) commandUnit;
    AbstractScriptExecutor executor =
        getExecutor(sshCommandTaskParameters, scriptCommandUnit, commandUnitsProgress, logStreamingTaskClient);

    return executor.executeCommandString(scriptCommandUnit.getCommand());
  }

  private ScriptSshExecutor getScriptSshExecutor(SshCommandTaskParameters parameters, ScriptCommandUnit commandUnit,
      CommandUnitsProgress commandUnitsProgress, ILogStreamingTaskClient logStreamingTaskClient) {
    SshSessionConfig sshSessionConfig = SshUtils.generateSshSessionConfig(
        sshSessionConfigMapper, parameters, commandUnit.getName(), commandUnit.getWorkingDirectory());
    return sshExecutorFactoryNG.getExecutor(sshSessionConfig, logStreamingTaskClient, commandUnitsProgress);
  }

  private ScriptProcessExecutor getScriptProcessExecutor(SshCommandTaskParameters parameters,
      ScriptCommandUnit commandUnit, CommandUnitsProgress commandUnitsProgress,
      ILogStreamingTaskClient logStreamingTaskClient) {
    ShellExecutorConfig config = getShellExecutorConfig(parameters, commandUnit);
    return shellExecutorFactory.getExecutor(config, logStreamingTaskClient, commandUnitsProgress);
  }

  private ShellExecutorConfig getShellExecutorConfig(
      SshCommandTaskParameters taskParameters, ScriptCommandUnit commandUnit) {
    return ShellExecutorConfig.builder()
        .accountId(taskParameters.getAccountId())
        .executionId(taskParameters.getExecutionId())
        .commandUnitName(commandUnit.getName())
        .workingDirectory(commandUnit.getWorkingDirectory())
        .environment(taskParameters.getEnvironmentVariables())
        .scriptType(ScriptType.BASH)
        .build();
  }

  private AbstractScriptExecutor getExecutor(SshCommandTaskParameters parameters, ScriptCommandUnit commandUnit,
      CommandUnitsProgress commandUnitsProgress, ILogStreamingTaskClient logStreamingTaskClient) {
    return parameters.isExecuteOnDelegate()
        ? getScriptProcessExecutor(parameters, commandUnit, commandUnitsProgress, logStreamingTaskClient)
        : getScriptSshExecutor(parameters, commandUnit, commandUnitsProgress, logStreamingTaskClient);
  }
}
