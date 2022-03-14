package io.harness.delegate.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.shell.ExecuteCommandResponse;

import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
@OwnedBy(CDP)
public class CommandTaskResponse implements DelegateResponseData {
  @NonFinal @Setter DelegateMetaInfo delegateMetaInfo;
  ExecuteCommandResponse executeCommandResponse;
  CommandExecutionStatus status;
  String errorMessage;
  UnitProgressData unitProgressData;
}
