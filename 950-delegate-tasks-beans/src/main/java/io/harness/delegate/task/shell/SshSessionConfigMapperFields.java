package io.harness.delegate.task.shell;

import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface SshSessionConfigMapperFields {
  String getAccountId();
  String getExecutionId();
  String getHost();
  String getWorkingDirectory();
  SSHKeySpecDTO getSshKeySpecDTO();
  List<EncryptedDataDetail> getEncryptionDetails();
}
