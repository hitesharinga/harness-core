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
