package io.harness.pms.helpers;

import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.exception.InvalidRequestException;
import io.harness.jackson.JsonNodeUtils;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.pms.pipeline.CloneIdentifierConfig;
import io.harness.pms.pipeline.ClonePipelineDTO;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.rbac.PipelineRbacPermissions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class PipelineCloneHelper {
  private final AccessControlClient accessControlClient;
  private final PMSPipelineService pmsPipelineService;

  public void checkAccess(ClonePipelineDTO clonePipelineDTO, String accountId) {
    CloneIdentifierConfig destIdentifierConfig = clonePipelineDTO.getDestination();
    CloneIdentifierConfig sourceIdentifierConfig = clonePipelineDTO.getSource();

    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, destIdentifierConfig.getOrgIdentifier(),
                                                  destIdentifierConfig.getProjectIdentifier()),
        Resource.of("PIPELINE", destIdentifierConfig.getPipelineIdentifier()),
        PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT);

    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, sourceIdentifierConfig.getOrgIdentifier(),
                                                  sourceIdentifierConfig.getProjectIdentifier()),
        Resource.of("PIPELINE", sourceIdentifierConfig.getPipelineIdentifier()), PipelineRbacPermissions.PIPELINE_VIEW);
  }

  public String getDestYamlfromSource(ClonePipelineDTO clonePipelineDTO, String accountId) {
    Optional<PipelineEntity> sourcePipelineEntity = pmsPipelineService.get(accountId,
        clonePipelineDTO.getSource().getOrgIdentifier(), clonePipelineDTO.getSource().getProjectIdentifier(),
        clonePipelineDTO.getSource().getPipelineIdentifier(), false);

    if (!sourcePipelineEntity.isPresent()) {
      log.error(String.format("Pipeline with id [%s] in org [%s] in project [%s] is not present or deleted",
          clonePipelineDTO.getSource().getPipelineIdentifier(), clonePipelineDTO.getSource().getOrgIdentifier(),
          clonePipelineDTO.getSource().getProjectIdentifier()));
      throw new InvalidRequestException(
          String.format("Pipeline with id [%s] in org [%s] in project [%s] is not present or deleted",
              clonePipelineDTO.getSource().getPipelineIdentifier(), clonePipelineDTO.getSource().getOrgIdentifier(),
              clonePipelineDTO.getSource().getProjectIdentifier()));
    }

    String sourcePipelineEntityYaml = sourcePipelineEntity.get().getYaml();
    String destOrgId = clonePipelineDTO.getDestination().getOrgIdentifier();
    String destProjectId = clonePipelineDTO.getDestination().getProjectIdentifier();
    String destPipelineName = clonePipelineDTO.getDestination().getPipelineName();

    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    JsonNode jsonNode;
    try {
      jsonNode = objectMapper.readTree(sourcePipelineEntityYaml);
    } catch (JsonProcessingException e) {
      log.error(String.format("Error while processing source yaml to json for pipeline [%s]",
                    sourcePipelineEntity.get().getIdentifier()),
          e);
      throw new InvalidRequestException(
          String.format("Generic Backend Error occurred for pipeline [%s] org [%s] project [%s]",
              clonePipelineDTO.getSource().getPipelineIdentifier(), clonePipelineDTO.getSource().getOrgIdentifier(),
              clonePipelineDTO.getSource().getProjectIdentifier()),
          e);
    }

    // Resolve source yaml Params
    if (destProjectId != null && !destProjectId.equals(clonePipelineDTO.getSource().getProjectIdentifier())) {
      JsonNodeUtils.updatePropertyInObjectNode(jsonNode.get("pipeline"), "projectIdentifier", destProjectId);
    }
    if (destOrgId != null && !destOrgId.equals(clonePipelineDTO.getSource().getOrgIdentifier())) {
      JsonNodeUtils.updatePropertyInObjectNode(jsonNode.get("pipeline"), "orgIdentifier", destOrgId);
    }
    if (clonePipelineDTO.getDestination().getPipelineIdentifier() != null
        && !clonePipelineDTO.getDestination().getPipelineIdentifier().equals(
            clonePipelineDTO.getSource().getPipelineIdentifier())) {
      JsonNodeUtils.updatePropertyInObjectNode(
          jsonNode.get("pipeline"), "identifier", clonePipelineDTO.getDestination().getPipelineIdentifier());
    }
    if (clonePipelineDTO.getDestination().getDescription() != null) {
      JsonNodeUtils.updatePropertyInObjectNode(
          jsonNode.get("pipeline"), "description", clonePipelineDTO.getDestination().getDescription());
    }
    if (clonePipelineDTO.getDestination().getTags() != null) {
      List<NGTag> tags = clonePipelineDTO.getDestination().getTags();

      JsonNodeUtils.upsertPropertiesInJsonNode(
          (ObjectNode) jsonNode.get("pipeline").get("tags"), TagMapper.convertToMap(tags));
    }
    if (destPipelineName != null) {
      JsonNodeUtils.updatePropertyInObjectNode(jsonNode.get("pipeline"), "name", destPipelineName);
    } else {
      log.error(String.format("Error Destination Pipeline is null for pipeline [%s]",
          clonePipelineDTO.getDestination().getPipelineIdentifier()));
      throw new InvalidRequestException(String.format("Destination Pipeline Name should not be null for pipeline [%s]",
          clonePipelineDTO.getDestination().getPipelineIdentifier()));
    }

    String modifiedSourceYaml;
    try {
      modifiedSourceYaml = new YAMLMapper().writeValueAsString(jsonNode);
    } catch (IOException e) {
      log.error(
          String.format("Unable to convert json to yaml for pipeline [%s] org [%s] project [%s]",
              clonePipelineDTO.getSource().getPipelineIdentifier(), clonePipelineDTO.getSource().getOrgIdentifier(),
              clonePipelineDTO.getSource().getProjectIdentifier()),
          e);
      throw new InvalidRequestException(
          String.format("Generic Backend Error occurred for pipeline [%s] org [%s] project [%s]",
              clonePipelineDTO.getSource().getPipelineIdentifier(), clonePipelineDTO.getSource().getOrgIdentifier(),
              clonePipelineDTO.getSource().getProjectIdentifier()),
          e);
    }
    return modifiedSourceYaml;
  }
}
