/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.task.shell.SshSessionConfigMapper;
import io.harness.delegate.task.shell.SshCommandTaskParameters;
import io.harness.delegate.task.shell.SshSessionConfigMapperFields;
import io.harness.shell.SshSessionConfig;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SshUtils {
  public static SshSessionConfig generateSshSessionConfig(
      SshSessionConfigMapper sshSessionConfigMapper, SshSessionConfigMapperFields fields, String commandUnit) {
    SshSessionConfig sshSessionConfig =
        sshSessionConfigMapper.getSSHSessionConfig(fields.getSshKeySpecDTO(), fields.getEncryptionDetails());

    sshSessionConfig.setAccountId(fields.getAccountId());
    sshSessionConfig.setExecutionId(fields.getExecutionId());
    sshSessionConfig.setHost(fields.getHost());
    sshSessionConfig.setWorkingDirectory(fields.getWorkingDirectory());
    sshSessionConfig.setCommandUnitName(commandUnit);
    return sshSessionConfig;
  }

  public static SshSessionConfig generateSshSessionConfig(
      SshSessionConfigMapper sshSessionConfigMapper, SshCommandTaskParameters taskParameters, String commandUnit) {
    SshSessionConfig sshSessionConfig =
        sshSessionConfigMapper.getSSHSessionConfig(taskParameters.getSshInfraDelegateConfig().getSshKeySpecDto(),
            taskParameters.getSshInfraDelegateConfig().getEncryptionDataDetails());

    sshSessionConfig.setAccountId(taskParameters.getAccountId());
    sshSessionConfig.setExecutionId(taskParameters.getExecutionId());
    sshSessionConfig.setHost(taskParameters.getSshInfraDelegateConfig().getHosts().get(0));
    sshSessionConfig.setWorkingDirectory(taskParameters.getWorkingDirectory());
    sshSessionConfig.setCommandUnitName(commandUnit);

    return sshSessionConfig;
  }
}
