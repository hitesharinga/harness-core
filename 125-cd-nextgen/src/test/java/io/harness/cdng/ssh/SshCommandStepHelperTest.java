/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.ssh;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.PdcInfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.task.shell.SshCommandTaskParameters;
import io.harness.delegate.task.ssh.PdcSshInfraDelegateConfig;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.pms.data.OrchestrationRefType;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.shell.ScriptType;
import io.harness.steps.shellscript.ShellScriptHelperService;
import io.harness.steps.shellscript.ShellScriptInlineSource;
import io.harness.steps.shellscript.ShellScriptSourceWrapper;
import io.harness.steps.shellscript.ShellType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class SshCommandStepHelperTest extends CategoryTest {
  @Mock private ShellScriptHelperService shellScriptHelperService;
  @Mock private SshEntityHelper sshEntityHelper;
  @Mock private OutcomeService outcomeService;

  @InjectMocks private SshCommandStepHelper helper;

  private final String workingDir = "/tmp";
  private final String accountId = "test";
  private final Ambiance ambiance = Ambiance.newBuilder().putSetupAbstractions("accountId", accountId).build();
  private final RefObject infra = RefObject.newBuilder()
                                      .setName(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                                      .setKey(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
                                      .setRefType(RefType.newBuilder().setType(OrchestrationRefType.OUTCOME).build())
                                      .build();

  private final PdcInfrastructureOutcome pdcInfrastructure =
      PdcInfrastructureOutcome.builder().connectorRef("pdcConnector").credentialsRef("sshKeyRef").build();
  private final OptionalOutcome pdcInfrastructureOutcome =
      OptionalOutcome.builder()
          .found(true)
          .outcome(PdcInfrastructureOutcome.builder()
                       .credentialsRef(pdcInfrastructure.getCredentialsRef())
                       .connectorRef(pdcInfrastructure.getConnectorRef())
                       .build())
          .build();

  private final PdcSshInfraDelegateConfig pdcSshInfraDelegateConfig =
      PdcSshInfraDelegateConfig.builder().hosts(Arrays.asList("host1")).build();

  @Before
  public void prepare() {
    MockitoAnnotations.initMocks(this);
    doReturn(pdcInfrastructureOutcome).when(outcomeService).resolveOptional(eq(ambiance), eq(infra));
    doReturn(pdcSshInfraDelegateConfig).when(sshEntityHelper).getSshInfraDelegateConfig(pdcInfrastructure, ambiance);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testBuildSshCommandTaskParameters() {
    Map<String, Object> env = new LinkedHashMap<>();
    env.put("key", "val");

    Map<String, String> taskEnv = new LinkedHashMap<>();
    env.put("key", "val");

    ParameterField workingDirParam = ParameterField.createValueField(workingDir);
    ExecuteCommandStepParameters stepParameters =
        ExecuteCommandStepParameters.infoBuilder()
            .shell(ShellType.Bash)
            .environmentVariables(env)
            .workingDirectory(workingDirParam)
            .tailFiles(Arrays.asList(TailFilePattern.builder()
                                         .tailFile(ParameterField.createValueField("nohup.out"))
                                         .tailPattern(ParameterField.createValueField("*Successfull"))
                                         .build()))
            .delegateSelectors(ParameterField.createValueField(Arrays.asList(new TaskSelectorYaml("ssh-delegate"))))
            .onDelegate(ParameterField.createValueField(false))
            .source(
                ShellScriptSourceWrapper.builder()
                    .spec(
                        ShellScriptInlineSource.builder().script(ParameterField.createValueField("echo Test")).build())
                    .type("Inline")
                    .build())
            .build();

    doReturn(workingDir)
        .when(shellScriptHelperService)
        .getWorkingDirectory(eq(workingDirParam), any(ScriptType.class), anyBoolean());
    doReturn(taskEnv).when(shellScriptHelperService).getEnvironmentVariables(env);
    SshCommandTaskParameters taskParameters = helper.buildSshCommandTaskParameters(ambiance, stepParameters);
    assertThat(taskParameters).isNotNull();
    assertThat(taskParameters.getSshInfraDelegateConfig()).isEqualTo(pdcSshInfraDelegateConfig);
    assertThat(taskParameters.getAccountId()).isEqualTo(accountId);
    assertThat(taskParameters.getWorkingDirectory()).isEqualTo(workingDir);
    assertThat(taskParameters.getScript()).isEqualTo("echo Test");
    assertThat(taskParameters.getScriptType()).isEqualTo(ScriptType.BASH);
    assertThat(taskParameters.getTailFilePatterns().get(0).getFilePath()).isEqualTo("nohup.out");
    assertThat(taskParameters.getTailFilePatterns().get(0).getPattern()).isEqualTo("*Successfull");
    assertThat(taskParameters.getEnvironmentVariables()).isEqualTo(taskEnv);
  }
}
