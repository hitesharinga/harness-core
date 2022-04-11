/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.task.shell.SshSessionConfigMapper;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.ssh.PdcSshInfraDelegateConfig;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.rule.Owner;
import io.harness.shell.ExecuteCommandResponse;
import io.harness.shell.ScriptProcessExecutor;
import io.harness.shell.ScriptSshExecutor;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommandTaskNGTest extends CategoryTest {
  final DelegateTaskPackage delegateTaskPackage =
      DelegateTaskPackage.builder().data(TaskData.builder().build()).build();
  @Mock BooleanSupplier preExecute;
  @Mock Consumer<DelegateTaskResponse> consumer;
  @Mock ILogStreamingTaskClient logStreamingTaskClient;

  @Mock SshExecutorFactoryNG sshExecutorFactoryNG;
  @Mock ShellExecutorFactoryNG shellExecutorFactory;
  @Mock SshSessionConfigMapper sshSessionConfigMapper;
  @Mock SshInitCommandHandler sshInitCommandHandler;
  @Mock SshCleanupCommandHandler sshCleanupCommandHandler;

  @Mock ScriptSshExecutor scriptSshExecutor;
  @Mock ScriptProcessExecutor scriptProcessExecutor;

  final ExecuteCommandResponse successResponse =
      ExecuteCommandResponse.builder().status(CommandExecutionStatus.SUCCESS).build();
  final ExecuteCommandResponse failureResponse =
      ExecuteCommandResponse.builder().status(CommandExecutionStatus.FAILURE).build();
  SshSessionConfig sshSessionConfig = SshSessionConfig.Builder.aSshSessionConfig().build();

  @Inject
  @InjectMocks
  CommandTaskNG task = new CommandTaskNG(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  ;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    doReturn(sshSessionConfig).when(sshSessionConfigMapper).getSSHSessionConfig(any(), anyList());
  }

  @Test(expected = NotImplementedException.class)
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldAcceptOnlyTaskParams() {
    task.run(new Object[] {});
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testExecuteSshCommandTaskOnHostSuccess() {
    String script = "echo Test";
    SshCommandTaskParameters taskParameters =
        SshCommandTaskParameters.builder()
            .executeOnDelegate(false)
            .sshInfraDelegateConfig(PdcSshInfraDelegateConfig.builder()
                                        .sshKeySpecDto(SSHKeySpecDTO.builder().build())
                                        .encryptionDataDetails(Collections.emptyList())
                                        .hosts(Arrays.asList("host1"))
                                        .build())
            .accountId("accountId")
            .executionId("executionId")
            .workingDirectory("/tmp")
            .build();
    doReturn(scriptSshExecutor).when(sshExecutorFactoryNG).getExecutor(any(), any(), any());
    doReturn(script).when(sshInitCommandHandler).prepareScript(eq(taskParameters), eq(scriptSshExecutor));
    doReturn(successResponse).when(scriptSshExecutor).executeCommandString(eq(script), eq(Collections.emptyList()));
    doNothing().when(sshCleanupCommandHandler).cleanup(eq(taskParameters), eq(scriptSshExecutor));

    DelegateResponseData responseData = task.run(taskParameters);
    assertThat(responseData).isInstanceOf(CommandTaskResponse.class);
    CommandTaskResponse commandTaskResponse = (CommandTaskResponse) responseData;
    assertThat(commandTaskResponse.getStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    verify(sshExecutorFactoryNG, times(3)).getExecutor(any(), any(), any());
    verify(sshInitCommandHandler, times(1)).prepareScript(eq(taskParameters), eq(scriptSshExecutor));
    verify(scriptSshExecutor, times(1)).executeCommandString(eq(script), eq(Collections.emptyList()));
    verify(sshCleanupCommandHandler, times(1)).cleanup(eq(taskParameters), eq(scriptSshExecutor));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testExecuteSshCommandTaskOnHostFailure() {
    String script = "echo Test";
    SshCommandTaskParameters taskParameters =
        SshCommandTaskParameters.builder()
            .executeOnDelegate(false)
            .sshInfraDelegateConfig(PdcSshInfraDelegateConfig.builder()
                                        .sshKeySpecDto(SSHKeySpecDTO.builder().build())
                                        .encryptionDataDetails(Collections.emptyList())
                                        .hosts(Arrays.asList("host1"))
                                        .build())
            .accountId("accountId")
            .executionId("executionId")
            .workingDirectory("/tmp")
            .build();
    doReturn(scriptSshExecutor).when(sshExecutorFactoryNG).getExecutor(any(), any(), any());
    doReturn(script).when(sshInitCommandHandler).prepareScript(eq(taskParameters), eq(scriptSshExecutor));
    doThrow(new RuntimeException("failed to execute script"))
        .when(scriptSshExecutor)
        .executeCommandString(eq(script), eq(Collections.emptyList()));

    DelegateResponseData responseData = task.run(taskParameters);
    assertThat(responseData).isInstanceOf(CommandTaskResponse.class);
    CommandTaskResponse commandTaskResponse = (CommandTaskResponse) responseData;
    assertThat(commandTaskResponse.getStatus()).isEqualTo(CommandExecutionStatus.FAILURE);

    verify(sshExecutorFactoryNG, times(2)).getExecutor(any(), any(), any());
    verify(sshInitCommandHandler, times(1)).prepareScript(eq(taskParameters), eq(scriptSshExecutor));
    verify(scriptSshExecutor, times(1)).executeCommandString(eq(script), eq(Collections.emptyList()));
    verify(sshCleanupCommandHandler, times(0)).cleanup(eq(taskParameters), eq(scriptSshExecutor));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testExecuteSshCommandTaskOnDelegateSuccess() {
    String script = "echo Test";
    SshCommandTaskParameters taskParameters =
        SshCommandTaskParameters.builder()
            .executeOnDelegate(true)
            .sshInfraDelegateConfig(PdcSshInfraDelegateConfig.builder()
                                        .sshKeySpecDto(SSHKeySpecDTO.builder().build())
                                        .encryptionDataDetails(Collections.emptyList())
                                        .hosts(Arrays.asList("host1"))
                                        .build())
            .accountId("accountId")
            .executionId("executionId")
            .workingDirectory("/tmp")
            .build();
    doReturn(scriptProcessExecutor).when(shellExecutorFactory).getExecutor(any(), any(), any());
    doReturn(script).when(sshInitCommandHandler).prepareScript(eq(taskParameters), eq(scriptProcessExecutor));
    doReturn(successResponse).when(scriptProcessExecutor).executeCommandString(eq(script), eq(Collections.emptyList()));
    doNothing().when(sshCleanupCommandHandler).cleanup(eq(taskParameters), eq(scriptProcessExecutor));

    DelegateResponseData responseData = task.run(taskParameters);
    assertThat(responseData).isInstanceOf(CommandTaskResponse.class);
    CommandTaskResponse commandTaskResponse = (CommandTaskResponse) responseData;
    assertThat(commandTaskResponse.getStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);

    verify(shellExecutorFactory, times(3)).getExecutor(any(), any(), any());
    verify(sshInitCommandHandler, times(1)).prepareScript(eq(taskParameters), eq(scriptProcessExecutor));
    verify(scriptProcessExecutor, times(1)).executeCommandString(eq(script), eq(Collections.emptyList()));
    verify(sshCleanupCommandHandler, times(1)).cleanup(eq(taskParameters), eq(scriptProcessExecutor));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testExecuteSshCommandTaskOnDelegateFailure() {
    String script = "echo Test";
    SshCommandTaskParameters taskParameters =
        SshCommandTaskParameters.builder()
            .executeOnDelegate(true)
            .sshInfraDelegateConfig(PdcSshInfraDelegateConfig.builder()
                                        .sshKeySpecDto(SSHKeySpecDTO.builder().build())
                                        .encryptionDataDetails(Collections.emptyList())
                                        .hosts(Arrays.asList("host1"))
                                        .build())
            .accountId("accountId")
            .executionId("executionId")
            .workingDirectory("/tmp")
            .build();
    doReturn(scriptProcessExecutor).when(shellExecutorFactory).getExecutor(any(), any(), any());
    doReturn(script).when(sshInitCommandHandler).prepareScript(eq(taskParameters), eq(scriptProcessExecutor));
    doThrow(new RuntimeException("failed to execute script on delegate"))
        .when(scriptProcessExecutor)
        .executeCommandString(eq(script), eq(Collections.emptyList()));

    DelegateResponseData responseData = task.run(taskParameters);
    assertThat(responseData).isInstanceOf(CommandTaskResponse.class);
    CommandTaskResponse commandTaskResponse = (CommandTaskResponse) responseData;
    assertThat(commandTaskResponse.getStatus()).isEqualTo(CommandExecutionStatus.FAILURE);

    verify(shellExecutorFactory, times(2)).getExecutor(any(), any(), any());
    verify(sshInitCommandHandler, times(1)).prepareScript(eq(taskParameters), eq(scriptProcessExecutor));
    verify(scriptProcessExecutor, times(1)).executeCommandString(eq(script), eq(Collections.emptyList()));
    verify(sshCleanupCommandHandler, times(0)).cleanup(eq(taskParameters), eq(scriptProcessExecutor));
  }
}