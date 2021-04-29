package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.product.ci.scm.proto.UpdateFileResponse;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.service.ScmServiceClient;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(HarnessTeam.DX)
public class ScmPushTask extends AbstractDelegateRunnableTask {
  @Inject private SecretDecryptionService decryptionService;
  @Inject ScmDelegateClient scmDelegateClient;
  @Inject ScmServiceClient scmServiceClient;

  public ScmPushTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ScmPushTaskParams scmPushTaskParams = (ScmPushTaskParams) parameters;
    ConnectorConfigDTO scmConnector = (ConnectorConfigDTO) scmPushTaskParams.getScmConnector();
    decryptionService.decrypt(
        scmConnector.getDecryptableEntities().get(1), scmPushTaskParams.getEncryptedDataDetails());
    switch (scmPushTaskParams.changeType) {
      case ADD: {
        CreateFileResponse createFileResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.createFile(
                scmPushTaskParams.scmConnector, scmPushTaskParams.gitFileDetails, SCMGrpc.newBlockingStub(c)));
        return ScmPushTaskResponseData.builder()
            .createFileResponse(createFileResponse)
            .changeType(scmPushTaskParams.changeType)
            .build();
      }
      case DELETE: {
        DeleteFileResponse deleteFileResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.deleteFile(scmPushTaskParams.scmConnector,
                GitFilePathDetails.builder()
                    .branch(scmPushTaskParams.gitFileDetails.getBranch())
                    .filePath(scmPushTaskParams.gitFileDetails.getFilePath())
                    .build(),
                SCMGrpc.newBlockingStub(c)));
        return ScmPushTaskResponseData.builder()
            .deleteFileResponse(deleteFileResponse)
            .changeType(scmPushTaskParams.changeType)
            .build();
      }
      case MODIFY: {
        UpdateFileResponse updateFileResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.updateFile(
                scmPushTaskParams.scmConnector, scmPushTaskParams.gitFileDetails, SCMGrpc.newBlockingStub(c)));
        return ScmPushTaskResponseData.builder()
            .updateFileResponse(updateFileResponse)
            .changeType(scmPushTaskParams.changeType)
            .build();
      }
      case RENAME:
      case NONE:
        throw new NotImplementedException("Not Implemented");
      default: {
        throw new NotImplementedException("Not Implemented");
      }
    }
  }
}
