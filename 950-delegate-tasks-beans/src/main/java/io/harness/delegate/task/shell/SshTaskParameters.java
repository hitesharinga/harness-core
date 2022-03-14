package io.harness.delegate.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Value
@OwnedBy(CDP)
public class SshTaskParameters extends CommandTaskParameters {
  SSHKeySpecDTO sshKeySpecDTO;
  List<EncryptedDataDetail> encryptionDetails;
}
