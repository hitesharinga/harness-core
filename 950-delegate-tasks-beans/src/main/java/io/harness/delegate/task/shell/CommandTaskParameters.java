/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
  boolean executeOnDelegate;
}
