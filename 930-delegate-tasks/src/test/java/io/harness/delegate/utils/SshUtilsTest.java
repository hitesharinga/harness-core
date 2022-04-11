/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.task.shell.SshSessionConfigMapper;
import io.harness.delegate.task.shell.ShellScriptTaskParametersNG;
import io.harness.delegate.task.shell.SshCommandTaskParameters;
import io.harness.delegate.task.ssh.PdcSshInfraDelegateConfig;
import io.harness.rule.Owner;
import io.harness.shell.SshSessionConfig;

import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class SshUtilsTest extends CategoryTest {
  @Mock private SshSessionConfigMapper sshSessionConfigMapper;
  private final String accountId = "testAccId";
  private final String executionId = "testExecutionId";
  private final String workingDir = "/tmp";
  private final String commandUnitName = "Test";
  private final String host = "host1";

  @Before
  public void prepare() throws IOException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGenerateSshConfigFromSshCommandParams() {
    SshSessionConfig sessionConfig = SshSessionConfig.Builder.aSshSessionConfig().build();
    doReturn(sessionConfig).when(sshSessionConfigMapper).getSSHSessionConfig(any(), anyList());

    SshCommandTaskParameters commandTaskParameters =
        SshCommandTaskParameters.builder()
            .accountId(accountId)
            .executionId(executionId)
            .workingDirectory(workingDir)
            .sshInfraDelegateConfig(PdcSshInfraDelegateConfig.builder().hosts(Arrays.asList(host)).build())
            .build();

    SshUtils.generateSshSessionConfig(sshSessionConfigMapper, commandTaskParameters, commandUnitName);
    assertThat(sessionConfig.getAccountId()).isEqualTo(accountId);
    assertThat(sessionConfig.getExecutionId()).isEqualTo(executionId);
    assertThat(sessionConfig.getWorkingDirectory()).isEqualTo(workingDir);
    assertThat(sessionConfig.getCommandUnitName()).isEqualTo(commandUnitName);
    assertThat(sessionConfig.getHost()).isEqualTo(host);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGenerateSshConfigFromShellScriptParams() {
    SshSessionConfig sessionConfig = SshSessionConfig.Builder.aSshSessionConfig().build();
    doReturn(sessionConfig).when(sshSessionConfigMapper).getSSHSessionConfig(any(), anyList());

    ShellScriptTaskParametersNG shellScriptTaskParameters = ShellScriptTaskParametersNG.builder()
                                                                .accountId(accountId)
                                                                .executionId(executionId)
                                                                .workingDirectory(workingDir)
                                                                .host(host)
                                                                .build();

    SshUtils.generateSshSessionConfig(sshSessionConfigMapper, shellScriptTaskParameters, commandUnitName);
    assertThat(sessionConfig.getAccountId()).isEqualTo(accountId);
    assertThat(sessionConfig.getExecutionId()).isEqualTo(executionId);
    assertThat(sessionConfig.getWorkingDirectory()).isEqualTo(workingDir);
    assertThat(sessionConfig.getCommandUnitName()).isEqualTo(commandUnitName);
    assertThat(sessionConfig.getHost()).isEqualTo(host);
  }
}
