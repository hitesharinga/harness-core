package software.wings.sm;

import java.io.Serializable;

/**
 * Interface defining StateMachine execution callback.
 *
 * @author Rishi
 */
public interface StateMachineExecutionCallback extends Serializable {
  public void callback(ExecutionContext context, ExecutionStatus status, Exception ex);
}
