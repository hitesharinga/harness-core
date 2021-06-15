package io.harness.pms.expressions.utils;

import static io.harness.k8s.model.ImageDetails.ImageDetailsBuilder;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.artifacts.ArtifactDelegateRequestUtils;
import io.harness.delegate.task.artifacts.ArtifactSourceConstants;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.k8s.model.ImageDetails;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.artifact.bean.ArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.DockerArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.EcrArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.GcrArtifactOutcome;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;

@Singleton
@Slf4j
public class ImagePullSecretUtils {
  @Inject private EcrImagePullSecretHelper ecrImagePullSecretHelper;
  @Inject private ConnectorResourceClient connectorResourceClient;
  @Transient
  private static final String DOCKER_REGISTRY_CREDENTIAL_TEMPLATE =
      "{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}";

  public String getImagePullSecret(ArtifactOutcome artifactOutcome, Ambiance ambiance) {
    ImageDetailsBuilder imageDetailsBuilder = ImageDetails.builder();
    switch (artifactOutcome.getArtifactType()) {
      case ArtifactSourceConstants.DOCKER_REGISTRY_NAME:
        getImageDetailsFromDocker((DockerArtifactOutcome) artifactOutcome, imageDetailsBuilder, ambiance);
        break;
      case ArtifactSourceConstants.GCR_NAME:
        getImageDetailsFromGcr((GcrArtifactOutcome) artifactOutcome, imageDetailsBuilder, ambiance);
        break;
      case ArtifactSourceConstants.ECR_NAME:
        getImageDetailsFromEcr((EcrArtifactOutcome) artifactOutcome, imageDetailsBuilder, ambiance);
        break;
      default:
        throw new UnsupportedOperationException(
            String.format("Unknown Artifact Config type: [%s]", artifactOutcome.getArtifactType()));
    }
    ImageDetails imageDetails = imageDetailsBuilder.build();
    if (EmptyPredicate.isNotEmpty(imageDetails.getRegistryUrl()) && isNotBlank(imageDetails.getUsername())
        && isNotBlank(imageDetails.getPassword())) {
      return getArtifactRegistryCredentials(imageDetails);
    }
    return "";
  }

  public static String getArtifactRegistryCredentials(ImageDetails imageDetails) {
    return "${imageSecret.create(\"" + imageDetails.getRegistryUrl() + "\", \"" + imageDetails.getUsername() + "\", "
        + imageDetails.getPassword() + ")}";
  }

  private void getImageDetailsFromDocker(
      DockerArtifactOutcome dockerArtifactOutcome, ImageDetailsBuilder imageDetailsBuilder, Ambiance ambiance) {
    String connectorRef = dockerArtifactOutcome.getConnectorRef();
    ConnectorInfoDTO connectorDTO = getConnector(connectorRef, ambiance);
    DockerConnectorDTO connectorConfig = (DockerConnectorDTO) connectorDTO.getConnectorConfig();
    if (connectorConfig.getAuth() != null && connectorConfig.getAuth().getCredentials() != null
        && connectorConfig.getAuth().getAuthType() == DockerAuthType.USER_PASSWORD) {
      DockerUserNamePasswordDTO credentials = (DockerUserNamePasswordDTO) connectorConfig.getAuth().getCredentials();
      String passwordRef = credentials.getPasswordRef().toSecretRefStringValue();
      imageDetailsBuilder.username(credentials.getUsername());
      imageDetailsBuilder.password(getPasswordExpression(passwordRef, ambiance));
      imageDetailsBuilder.registryUrl(connectorConfig.getDockerRegistryUrl());
    }
  }

  private void getImageDetailsFromGcr(
      GcrArtifactOutcome gcrArtifactOutcome, ImageDetailsBuilder imageDetailsBuilder, Ambiance ambiance) {
    String connectorRef = gcrArtifactOutcome.getConnectorRef();
    ConnectorInfoDTO connectorDTO = getConnector(connectorRef, ambiance);
    GcpConnectorDTO connectorConfig = (GcpConnectorDTO) connectorDTO.getConnectorConfig();
    String imageName = gcrArtifactOutcome.getRegistryHostname() + "/" + gcrArtifactOutcome.getImagePath();
    imageDetailsBuilder.registryUrl(imageName);
    imageDetailsBuilder.username("_json_key");
    if (connectorConfig.getCredential() != null
        && connectorConfig.getCredential().getGcpCredentialType() == GcpCredentialType.MANUAL_CREDENTIALS) {
      GcpManualDetailsDTO config = (GcpManualDetailsDTO) connectorConfig.getCredential().getConfig();
      imageDetailsBuilder.password(getPasswordExpression(config.getSecretKeyRef().toSecretRefStringValue(), ambiance));
    }
  }

  private void getImageDetailsFromEcr(
      EcrArtifactOutcome ecrArtifactOutcome, ImageDetailsBuilder imageDetailsBuilder, Ambiance ambiance) {
    String connectorRef = ecrArtifactOutcome.getConnectorRef();
    BaseNGAccess baseNGAccess = ecrImagePullSecretHelper.getBaseNGAccess(AmbianceHelper.getAccountId(ambiance),
        AmbianceHelper.getOrgIdentifier(ambiance), AmbianceHelper.getProjectIdentifier(ambiance));
    ConnectorInfoDTO connectorIntoDTO = getConnector(connectorRef, ambiance);
    AwsConnectorDTO connectorDTO = (AwsConnectorDTO) connectorIntoDTO.getConnectorConfig();
    List<EncryptedDataDetail> encryptionDetails =
        ecrImagePullSecretHelper.getEncryptionDetails(connectorDTO, baseNGAccess);
    EcrArtifactDelegateRequest ecrRequest = ArtifactDelegateRequestUtils.getEcrDelegateRequest(
        ecrArtifactOutcome.getImagePath(), ecrArtifactOutcome.getTag(), null, null, ecrArtifactOutcome.getRegion(),
        connectorRef, connectorDTO, encryptionDetails, ArtifactSourceType.ECR);
    ArtifactTaskExecutionResponse artifactTaskExecutionResponseForImageUrl = ecrImagePullSecretHelper.executeSyncTask(
        ambiance, ecrRequest, ArtifactTaskType.GET_IMAGE_URL, baseNGAccess, "Ecr Get image URL failure due to error");
    String imageUrl =
        ((EcrArtifactDelegateResponse) artifactTaskExecutionResponseForImageUrl.getArtifactDelegateResponses().get(0))
            .getImageUrl();
    ArtifactTaskExecutionResponse artifactTaskExecutionResponseForAuthToken = ecrImagePullSecretHelper.executeSyncTask(
        ambiance, ecrRequest, ArtifactTaskType.GET_AUTH_TOKEN, baseNGAccess, "Ecr Get Auth-token failure due to error");
    String authToken =
        ((EcrArtifactDelegateResponse) artifactTaskExecutionResponseForAuthToken.getArtifactDelegateResponses().get(0))
            .getAuthToken();
    String decoded = new String(Base64.getDecoder().decode(authToken));
    String password = decoded.split(":")[1];
    imageDetailsBuilder.name(imageUrl)
        .sourceName(ArtifactSourceType.ECR.getDisplayName())
        .registryUrl(imageUrlToRegistryUrl(imageUrl))
        .username("AWS");
    imageDetailsBuilder.password("\"" + password + "\"");
  }

  private String imageUrlToRegistryUrl(String imageUrl) {
    String fullImageUrl = "https://" + imageUrl + (imageUrl.endsWith("/") ? "" : "/");
    fullImageUrl = fullImageUrl.substring(0, fullImageUrl.length() - 1);
    int index = fullImageUrl.lastIndexOf('/');
    return fullImageUrl.substring(0, index + 1);
  }

  private ConnectorInfoDTO getConnector(String connectorIdentifierRef, Ambiance ambiance) {
    try {
      NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
      IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(connectorIdentifierRef,
          ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
      Optional<ConnectorDTO> connectorDTO =
          SafeHttpCall
              .execute(connectorResourceClient.get(connectorRef.getIdentifier(), connectorRef.getAccountIdentifier(),
                  connectorRef.getOrgIdentifier(), connectorRef.getProjectIdentifier()))
              .getData();
      if (!connectorDTO.isPresent()) {
        throw new InvalidRequestException(
            String.format("Connector not found for identifier : [%s]", connectorIdentifierRef), WingsException.USER);
      }
      return connectorDTO.get().getConnectorInfo();
    } catch (Exception e) {
      log.error(format("Unable to get connector information : [%s] ", connectorIdentifierRef), e);
      throw new InvalidRequestException(format("Unable to get connector information : [%s] ", connectorIdentifierRef));
    }
  }

  private String getPasswordExpression(String passwordRef, Ambiance ambiance) {
    return "${ngSecretManager.obtain(\"" + passwordRef + "\", " + ambiance.getExpressionFunctorToken() + ")}";
  }
}
