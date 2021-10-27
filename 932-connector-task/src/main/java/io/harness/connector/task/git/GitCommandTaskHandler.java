package io.harness.connector.task.git;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.connector.scm.github.GithubApiAccessType.GITHUB_APP;
import static io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus.SUCCESS;
import static io.harness.impl.ScmResponseStatusUtils.convertScmStatusCodeToErrorCode;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.apache.commons.lang3.StringUtils.stripStart;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cistatus.service.GithubAppConfig;
import io.harness.cistatus.service.GithubService;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.ManagerExecutable;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.connector.service.git.NGGitService;
import io.harness.connector.service.scm.ScmDelegateClient;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.eraro.ErrorCode;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.runtime.SCMRuntimeException;
import io.harness.git.GitClientHelper;
import io.harness.product.ci.scm.proto.GetUserReposResponse;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.service.ScmClient;
import io.harness.service.ScmServiceClient;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.DX)
public class GitCommandTaskHandler {
  @Inject private NGGitService gitService;
  @Inject private GithubService gitHubService;
  @Inject private NGErrorHelper ngErrorHelper;
  @Inject private ScmDelegateClient scmDelegateClient;
  @Inject private ScmServiceClient scmServiceClient;
  @Inject private ScmClient scmClient;

  public ConnectorValidationResult validateGitCredentials(GitConfigDTO gitConnector, ScmConnector scmConnector,
      String accountIdentifier, SshSessionConfig sshSessionConfig) {
    GitCommandExecutionResponse delegateResponseData;
    try {
      delegateResponseData = (GitCommandExecutionResponse) handleValidateTask(
          gitConnector, scmConnector, accountIdentifier, sshSessionConfig);
    } catch (Exception e) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .testedAt(System.currentTimeMillis())
          .errorSummary(ngErrorHelper.getErrorSummary(e.getMessage()))
          .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(e.getMessage())))
          .build();
    }
    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(delegateResponseData.getConnectorValidationResult().getTestedAt())
        .build();
  }

  public DelegateResponseData handleValidateTask(
      GitConfigDTO gitConfig, ScmConnector scmConnector, String accountId, SshSessionConfig sshSessionConfig) {
    log.info("Processing Git command: VALIDATE");
    handleGitValidation(gitConfig, accountId, sshSessionConfig);
    Boolean executeOnManager = Boolean.FALSE;
    if (gitConfig instanceof ManagerExecutable) {
      executeOnManager = gitConfig.getExecuteOnManager();
    }

    handleApiAccessValidation(scmConnector, executeOnManager);
    return GitCommandExecutionResponse.builder()
        .gitCommandStatus(SUCCESS)
        .connectorValidationResult(ConnectorValidationResult.builder()
                                       .testedAt(System.currentTimeMillis())
                                       .status(ConnectivityStatus.SUCCESS)
                                       .build())
        .build();
  }

  private void handleGitValidation(GitConfigDTO gitConfig, String accountId, SshSessionConfig sshSessionConfig) {
    if (gitConfig.getGitConnectionType() == GitConnectionType.ACCOUNT) {
      if (isNotEmpty(gitConfig.getValidationRepo())) {
        String url = format("%s/%s", stripEnd(gitConfig.getUrl(), "/"), stripStart(gitConfig.getValidationRepo(), "/"));
        gitConfig.setUrl(url);
      } else {
        return;
      }
    }
    gitService.validateOrThrow(gitConfig, accountId, sshSessionConfig);
  }

  private void handleApiAccessValidation(ScmConnector scmConnector, Boolean executeOnManager) {
    if (!GitApiAccessDecryptionHelper.hasApiAccess(scmConnector)) {
      return;
    }

    if (scmConnector instanceof GithubConnectorDTO) {
      GithubConnectorDTO gitHubConnector = (GithubConnectorDTO) scmConnector;
      if (gitHubConnector.getApiAccess().getType() == GITHUB_APP) {
        validateGitHubApp(gitHubConnector);
        return;
      }
    }

    GetUserReposResponse reposResponse;
    try {
      if (executeOnManager == Boolean.TRUE) {
        reposResponse = scmClient.getUserRepos(scmConnector);
      } else {
        reposResponse = scmDelegateClient.processScmRequest(
            c -> scmServiceClient.getUserRepos(scmConnector, SCMGrpc.newBlockingStub(c)));
      }
    } catch (Exception e) {
      throw SCMRuntimeException.builder().errorCode(ErrorCode.UNEXPECTED).cause(e).build();
    }
    if (reposResponse != null && reposResponse.getStatus() > 300) {
      ErrorCode errorCode = convertScmStatusCodeToErrorCode(reposResponse.getStatus());
      throw SCMRuntimeException.builder().errorCode(errorCode).message(reposResponse.getError()).build();
    }
  }

  private void validateGitHubApp(GithubConnectorDTO gitHubConnector) {
    GithubApiAccessDTO apiAccess = gitHubConnector.getApiAccess();
    GithubAppSpecDTO apiAccessDTO = (GithubAppSpecDTO) apiAccess.getSpec();
    try {
      gitHubService.getToken(GithubAppConfig.builder()
                                 .appId(apiAccessDTO.getApplicationId())
                                 .installationId(apiAccessDTO.getInstallationId())
                                 .privateKey(String.valueOf(apiAccessDTO.getPrivateKeyRef().getDecryptedValue()))
                                 .githubUrl(GitClientHelper.getGithubApiURL(gitHubConnector.getUrl()))
                                 .build());
    } catch (Exception e) {
      throw SCMRuntimeException.builder()
          .errorCode(ErrorCode.SCM_UNAUTHORIZED)
          .cause(e.getCause())
          .message(e.getCause().getLocalizedMessage())
          .build();
    }
  }
}
