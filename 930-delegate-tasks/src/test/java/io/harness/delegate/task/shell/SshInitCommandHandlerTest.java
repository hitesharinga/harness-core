/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.VITALIE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.CommandExecutionException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.shell.AbstractScriptExecutor;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@OwnedBy(CDP)
@RunWith(MockitoJUnitRunner.class)
public class SshInitCommandHandlerTest extends CategoryTest {
  static final String PRE_INIT_CMD = "mkdir -p /tmp/test";
  static final SshInitCommandHandler sshInitCommandTaskNG = new SshInitCommandHandler();

  @Mock AbstractScriptExecutor executor;

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void testPrepareScriptFails() {
    SshCommandTaskParameters parameters =
        SshCommandTaskParameters.builder().executionId("test").script("echo test").build();

    when(executor.executeCommandString(PRE_INIT_CMD, true)).thenReturn(CommandExecutionStatus.FAILURE);

    assertThatThrownBy(() -> {
      sshInitCommandTaskNG.prepareScript(parameters, executor);
    }).isInstanceOf(CommandExecutionException.class);
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void testPrepareScriptSuccessWithoutTail() {
    SshCommandTaskParameters parameters =
        SshCommandTaskParameters.builder().executionId("test").script("echo test").workingDirectory("/test").build();

    when(executor.executeCommandString(PRE_INIT_CMD, true)).thenReturn(CommandExecutionStatus.SUCCESS);

    String script = sshInitCommandTaskNG.prepareScript(parameters, executor);

    assertThat(script).contains(parameters.getScript());
    assertThat(script).contains("# set session");
    assertThat(script).doesNotContain("harness_utils_wait_for_tail_log_verification");
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void testPrepareScriptSuccessWithTail() {
    SshCommandTaskParameters parameters = SshCommandTaskParameters.builder()
                                              .executionId("test")
                                              .script("harness_utils_start_tail_log_verification()")
                                              .workingDirectory("/test")
                                              .build();

    when(executor.executeCommandString(PRE_INIT_CMD, true)).thenReturn(CommandExecutionStatus.SUCCESS);

    String script = sshInitCommandTaskNG.prepareScript(parameters, executor);

    assertThat(script).contains(parameters.getScript());
    assertThat(script).contains("harness_utils_start_tail_log_verification()");
    assertThat(script).contains("harness_utils_wait_for_tail_log_verification()");
    assertThat(script).doesNotContain("filePatterns=");
  }

  @Test
  @Owner(developers = VITALIE)
  @Category(UnitTests.class)
  public void testPrepareScriptSuccessWithTailPattern() {
    final String pattern = "some pattern";
    List<TailFilePatternDto> tailFilePatterns =
        Arrays.asList(TailFilePatternDto.builder().filePath("some path").pattern(pattern).build());

    SshCommandTaskParameters parameters = SshCommandTaskParameters.builder()
                                              .executionId("test")
                                              .script("test")
                                              .workingDirectory("/test")
                                              .tailFilePatterns(tailFilePatterns)
                                              .build();

    when(executor.executeCommandString(PRE_INIT_CMD, true)).thenReturn(CommandExecutionStatus.SUCCESS);

    String script = sshInitCommandTaskNG.prepareScript(parameters, executor);

    assertThat(script).contains(parameters.getScript());
    assertThat(script).contains("harness_utils_start_tail_log_verification()");
    assertThat(script).contains("harness_utils_wait_for_tail_log_verification()");
    assertThat(script).contains("filePatterns=");
    assertThat(script).contains(pattern);
  }
}
