package io.harness.gitsync.gitsyncerror.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.BHAVYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.gitsyncerror.GitSyncErrorStatus;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError.GitSyncErrorKeys;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorDetails;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;
import io.harness.gitsync.gitsyncerror.beans.GitToHarnessErrorDetails;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitToHarnessErrorByCommitResponseDTO;
import io.harness.ng.beans.PageRequest;
import io.harness.repositories.gitSyncError.GitSyncErrorRepository;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PL)
public class GitSyncErrorServiceImplTest extends GitSyncTestBase {
  private final String accountId = "accountId";
  private final String orgId = "orgId";
  private final String projectId = "projectId";
  private final String commitId = "commitId";
  private final String commitMessage = "message";
  private final GitSyncErrorType errorType = GitSyncErrorType.GIT_TO_HARNESS;
  private final GitSyncErrorStatus status = GitSyncErrorStatus.ACTIVE;
  private final String failureReason = "failed";
  private final String repoUrl = "repo";
  private final String branch = "branch";
  private final String repoId = "repoId";
  private GitSyncErrorDetails additionalErrorDetails;
  private YamlGitConfigDTO yamlGitConfigDTO;
  @Inject GitSyncErrorServiceImpl gitSyncErrorService;
  @Inject GitSyncErrorRepository gitSyncErrorRepository;
  @Mock YamlGitConfigService yamlGitConfigService;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    additionalErrorDetails =
        GitToHarnessErrorDetails.builder().gitCommitId(commitId).commitMessage(commitMessage).build();
    yamlGitConfigDTO = YamlGitConfigDTO.builder()
                           .branch(branch)
                           .repo(repoUrl)
                           .identifier(repoId)
                           .gitConnectorType(ConnectorType.GIT)
                           .build();
    when(yamlGitConfigService.get(any(), any(), any(), any())).thenReturn(yamlGitConfigDTO);
    FieldUtils.writeField(gitSyncErrorService, "yamlGitConfigService", yamlGitConfigService, true);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void test_listGitToHarnessErrorsGroupedByCommits() {
    long createdAt = OffsetDateTime.now().minusDays(12).toInstant().toEpochMilli();
    gitSyncErrorRepository.save(build("filepath1", additionalErrorDetails, createdAt));
    gitSyncErrorRepository.save(build("filePath2", additionalErrorDetails, createdAt));
    additionalErrorDetails =
        GitToHarnessErrorDetails.builder().gitCommitId("commit2").commitMessage(commitMessage).build();
    createdAt = OffsetDateTime.now().toInstant().toEpochMilli();
    gitSyncErrorRepository.save(build("filepath3", additionalErrorDetails, createdAt));

    PageRequest pageRequest = PageRequest.builder().pageSize(10).pageIndex(0).build();
    doReturn(yamlGitConfigDTO).when(yamlGitConfigService).getByProjectIdAndRepo(any(), any(), any(), any());
    List<GitToHarnessErrorByCommitResponseDTO> dto =
        gitSyncErrorService
            .listGitToHarnessErrorsGroupedByCommits(pageRequest, accountId, orgId, projectId, null, null, null, 1)
            .getContent();

    assertThat(dto.size()).isEqualTo(2);
    assertThat(dto.get(0).getGitCommitId()).isEqualTo("commit2");
    assertThat(dto.get(1).getFailedCount()).isEqualTo(2);
    assertThat(dto.get(1).getErrorsForSummaryView().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void test_listAllGitToHarnessErrors() {
    long createdAt = OffsetDateTime.now().minusDays(12).toInstant().toEpochMilli();
    gitSyncErrorRepository.save(build("filepath1", additionalErrorDetails, createdAt));
    gitSyncErrorRepository.save(build("filePath2", additionalErrorDetails, createdAt));
    additionalErrorDetails =
        GitToHarnessErrorDetails.builder().gitCommitId("commit2").commitMessage(commitMessage).build();
    createdAt = OffsetDateTime.now().toInstant().toEpochMilli();
    gitSyncErrorRepository.save(build("filepath3", additionalErrorDetails, createdAt));

    SortOrder order =
        SortOrder.Builder.aSortOrder().withField(GitSyncErrorKeys.createdAt, SortOrder.OrderType.DESC).build();
    PageRequest pageRequest =
        PageRequest.builder().pageSize(10).pageIndex(0).sortOrders(ImmutableList.of(order)).build();
    doReturn(yamlGitConfigDTO).when(yamlGitConfigService).getByProjectIdAndRepo(any(), any(), any(), any());
    List<GitSyncErrorDTO> dto =
        gitSyncErrorService.listAllGitToHarnessErrors(pageRequest, accountId, orgId, projectId, null, repoId, branch)
            .getContent();

    assertThat(dto.size()).isEqualTo(3);
    assertThat(dto.get(0).getCompleteFilePath()).isEqualTo("filepath3");
    assertThat(dto.get(1).getRepoId()).isEqualTo(repoId);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void test_listGitToHarnessErrorsForACommit() {
    gitSyncErrorService.save(buildDTO("filePath1", additionalErrorDetails));
    gitSyncErrorService.save(buildDTO("filePath2", additionalErrorDetails));
    PageRequest pageRequest = PageRequest.builder().pageSize(10).pageIndex(0).build();
    List<GitSyncErrorDTO> dto =
        gitSyncErrorService
            .listGitToHarnessErrorsForCommit(pageRequest, commitId, accountId, orgId, projectId, repoId, branch)
            .getContent();

    assertThat(dto.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void test_save() {
    GitSyncErrorDTO dto = buildDTO("filePath", additionalErrorDetails);
    GitSyncErrorDTO savedDto = gitSyncErrorService.save(dto);
    assertThat(savedDto).isEqualTo(dto);
  }

  private GitSyncErrorDTO buildDTO(String filepath, GitSyncErrorDetails additionalErrorDetails) {
    return GitSyncErrorDTO.builder()
        .accountIdentifier(accountId)
        .errorType(errorType)
        .completeFilePath(filepath)
        .repoUrl(repoUrl)
        .branchName(branch)
        .status(status)
        .failureReason(failureReason)
        .additionalErrorDetails(additionalErrorDetails)
        .build();
  }

  private GitSyncError build(String filepath, GitSyncErrorDetails additionalErrorDetails, long createdAt) {
    return GitSyncError.builder()
        .accountIdentifier(accountId)
        .errorType(errorType)
        .completeFilePath(filepath)
        .repoUrl(repoUrl)
        .branchName(branch)
        .status(status)
        .failureReason(failureReason)
        .additionalErrorDetails(additionalErrorDetails)
        .createdAt(createdAt)
        .build();
  }
}