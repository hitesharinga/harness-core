/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.filestore.dto.mapper;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.filestore.dto.node.FileNodeDTO;
import io.harness.filestore.dto.node.FolderNodeDTO;
import io.harness.filestore.entities.NGFile;

import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class FileStoreNodeDTOMapper {
  public io.harness.filestore.dto.node.FileNodeDTO getFileNodeDTO(NGFile ngFile) {
    return FileNodeDTO.builder()
        .identifier(ngFile.getIdentifier())
        .parentIdentifier(ngFile.getParentIdentifier())
        .name(ngFile.getName())
        .fileUsage(ngFile.getFileUsage())
        .description(ngFile.getDescription())
        .tags(ngFile.getTags())
        .lastModifiedAt(ngFile.getLastModifiedAt())
        .lastModifiedBy(EmbeddedUserDTOMapper.fromEmbeddedUser(ngFile.getLastUpdatedBy()))
        .mimeType(ngFile.getMimeType())
        .build();
  }

  public io.harness.filestore.dto.node.FolderNodeDTO getFolderNodeDTO(NGFile ngFile) {
    return FolderNodeDTO.builder()
        .identifier(ngFile.getIdentifier())
        .parentIdentifier(ngFile.getParentIdentifier())
        .name(ngFile.getName())
        .lastModifiedAt(ngFile.getLastModifiedAt())
        .lastModifiedBy(EmbeddedUserDTOMapper.fromEmbeddedUser(ngFile.getLastUpdatedBy()))
        .build();
  }
}
