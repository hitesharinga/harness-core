/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.ssh;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.common.ParameterFieldHelper.getBooleanParameterFieldValue;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.CDStepHelper;
import io.harness.delegate.task.shell.SshCommandTaskParameters;
import io.harness.delegate.task.shell.SshCommandTaskParameters.SshCommandTaskParametersBuilder;
import io.harness.delegate.task.shell.TailFilePatternDto;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.remote.client.NGRestUtils;
import io.harness.secretmanagerclient.services.SshKeySpecDTOHelper;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.shell.ScriptType;
import io.harness.steps.shellscript.ExecutionTarget;
import io.harness.steps.shellscript.ShellScriptHelperService;
import io.harness.steps.shellscript.ShellScriptInlineSource;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

@Singleton
@OwnedBy(CDP)
public class SshCommandStepHelper extends CDStepHelper {
  @Inject private ShellScriptHelperService shellScriptHelperService;
  @Inject @Named("PRIVILEGED") private SecretNGManagerClient secretManagerClient;
  @Inject private SshKeySpecDTOHelper sshKeySpecDTOHelper;

  public SshCommandTaskParameters buildSshCommandTaskParameters(
      @Nonnull Ambiance ambiance, @Nonnull ExecuteCommandStepParameters executeCommandStepParameters) {
    ScriptType scriptType = executeCommandStepParameters.getShell().getScriptType();
    Boolean onDelegate = getBooleanParameterFieldValue(executeCommandStepParameters.onDelegate);
    ExecutionTarget executionTarget = executeCommandStepParameters.getExecutionTarget();
    SshCommandTaskParametersBuilder<?, ?> builder = SshCommandTaskParameters.builder();
    populateHostAndSshCredentials(ambiance, executionTarget, onDelegate, builder);
    return builder.accountId(AmbianceUtils.getAccountId(ambiance))
        .executeOnDelegate(onDelegate)
        .executionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
        .script(getShellScript(executeCommandStepParameters))
        .scriptType(executeCommandStepParameters.getShell().getScriptType())
        .workingDirectory(shellScriptHelperService.getWorkingDirectory(executionTarget, scriptType, onDelegate))
        .tailFilePatterns(mapTailFilePatterns(executeCommandStepParameters))
        .build();
  }

  private List<TailFilePatternDto> mapTailFilePatterns(@Nonnull ExecuteCommandStepParameters stepParameters) {
    if (isEmpty(stepParameters.getTailFiles())) {
      return Collections.emptyList();
    }

    return stepParameters.getTailFiles()
        .stream()
        .map(it
            -> TailFilePatternDto.builder()
                   .filePath(getParameterFieldValue(it.getTailFile()))
                   .pattern(getParameterFieldValue(it.getTailPattern()))
                   .build())
        .collect(Collectors.toList());
  }

  private String getShellScript(@Nonnull ExecuteCommandStepParameters stepParameters) {
    ShellScriptInlineSource shellScriptInlineSource = (ShellScriptInlineSource) stepParameters.getSource().getSpec();
    return (String) shellScriptInlineSource.getScript().fetchFinalValue();
  }

  public void populateHostAndSshCredentials(@Nonnull Ambiance ambiance, @Nonnull ExecutionTarget executionTarget,
      Boolean onDelegate, @Nonnull SshCommandTaskParametersBuilder builder) {
    if (!onDelegate) {
      validateExecutionTarget(executionTarget);
      String sshKeyRef = executionTarget.getConnectorRef().getValue();

      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(sshKeyRef, AmbianceUtils.getAccountId(ambiance),
              AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));
      String errorMSg = "No secret configured with identifier: " + sshKeyRef;
      SecretResponseWrapper secretResponseWrapper = NGRestUtils.getResponse(
          secretManagerClient.getSecret(identifierRef.getIdentifier(), identifierRef.getAccountIdentifier(),
              identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier()),
          errorMSg);
      if (secretResponseWrapper == null) {
        throw new InvalidRequestException(errorMSg);
      }
      SecretDTOV2 secret = secretResponseWrapper.getSecret();

      SSHKeySpecDTO secretSpec = (SSHKeySpecDTO) secret.getSpec();
      NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
      List<EncryptedDataDetail> sshKeyEncryptionDetails =
          sshKeySpecDTOHelper.getSSHKeyEncryptionDetails(secretSpec, ngAccess);

      builder.sshKeySpecDTO(secretSpec)
          .encryptionDetails(sshKeyEncryptionDetails)
          .host(executionTarget.getHost().getValue());
    }
  }

  private void validateExecutionTarget(ExecutionTarget executionTarget) {
    if (executionTarget == null) {
      throw new InvalidRequestException("Execution Target can't be empty with on delegate set to false");
    }
    if (ParameterField.isNull(executionTarget.getConnectorRef())
        || StringUtils.isEmpty(executionTarget.getConnectorRef().getValue())) {
      throw new InvalidRequestException("Connector Ref in Execution Target can't be empty");
    }
    if (ParameterField.isNull(executionTarget.getHost()) || StringUtils.isEmpty(executionTarget.getHost().getValue())) {
      throw new InvalidRequestException("Host in Execution Target can't be empty");
    }
  }
}
