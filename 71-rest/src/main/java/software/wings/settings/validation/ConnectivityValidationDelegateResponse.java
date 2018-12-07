package software.wings.settings.validation;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.protocol.DelegateTaskNotifyResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConnectivityValidationDelegateResponse extends DelegateTaskNotifyResponseData {
  private boolean valid;
  private String errorMessage;
  private ExecutionStatus executionStatus;
}