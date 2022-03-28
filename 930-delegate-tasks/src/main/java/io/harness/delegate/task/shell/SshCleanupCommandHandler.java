/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.CommandExecutionException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.shell.AbstractScriptExecutor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(CDP)
@Singleton
public class SshCleanupCommandHandler {
  @Inject private SshInitCommandHandler sshInitCommandHandler;

  public void cleanup(SshCommandTaskParameters taskParameters, AbstractScriptExecutor executor) {
    String cmd = String.format("rm -rf %s", sshInitCommandHandler.getExecutionStagingDir(taskParameters));
    CommandExecutionStatus status = executor.executeCommandString(cmd, true);
    if (CommandExecutionStatus.SUCCESS != status) {
      throw new CommandExecutionException("Failed to cleanup");
    }
  }
}
