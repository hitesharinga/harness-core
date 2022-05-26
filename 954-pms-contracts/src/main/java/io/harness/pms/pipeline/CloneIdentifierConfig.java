/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.pipeline;

import io.harness.ng.core.common.beans.NGTag;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class CloneIdentifierConfig {
  @NonNull private String orgIdentifier;
  @NonNull private String projectIdentifier;
  @NonNull private String pipelineIdentifier;
  private String pipelineName;
  private String description;
  private List<NGTag> tags;
}
