/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.utils;

import io.harness.connector.task.shell.SshSessionConfigMapper;
import io.harness.delegate.task.shell.SshSessionConfigMapperFields;
import io.harness.shell.SshSessionConfig;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
}
