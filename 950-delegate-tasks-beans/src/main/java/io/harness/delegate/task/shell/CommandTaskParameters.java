package io.harness.delegate.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.Expression;
import io.harness.shell.ScriptType;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@OwnedBy(CDP)
public abstract class CommandTaskParameters implements TaskParameters {
  @Expression(ALLOW_SECRETS) String script;
  String accountId;
  String executionId;
  String workingDirectory;
  @Expression(ALLOW_SECRETS) Map<String, String> environmentVariables;
  ScriptType scriptType;
  String host;
}
