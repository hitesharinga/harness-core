/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.variables;

import io.harness.cdng.ecs.EcsRunTaskStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;

import java.util.Collections;
import java.util.Set;

public class EcsRunTaskStepVariableCreator extends GenericStepVariableCreator<EcsRunTaskStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Collections.singleton(StepSpecTypeConstants.ECS_RUN_TASK);
  }

  @Override
  public Class<EcsRunTaskStepNode> getFieldClass() {
    return EcsRunTaskStepNode.class;
  }
}
