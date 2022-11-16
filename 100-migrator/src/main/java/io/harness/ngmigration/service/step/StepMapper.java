/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.step;

import io.harness.data.structure.CollectionUtils;
import io.harness.ngmigration.service.MigratorUtility;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.plancreator.steps.internal.PmsAbstractStepNode;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.timeout.Timeout;

import software.wings.yaml.workflow.StepYaml;

import java.util.Map;

public interface StepMapper {
  String getStepType();

  AbstractStepNode getSpec(StepYaml stepYaml);

  default ParameterField<Timeout> getTimeout(StepYaml stepYaml) {
    Map<String, Object> properties = getProperties(stepYaml);
    String timeoutString = properties.getOrDefault("stateTimeoutInMinutes", "10") + "m";
    return ParameterField.createValueField(Timeout.builder().timeoutString(timeoutString).build());
  }

  default String getDescription(StepYaml stepYaml) {
    Map<String, Object> properties = getProperties(stepYaml);
    return properties.getOrDefault("description", "").toString();
  }

  default Map<String, Object> getProperties(StepYaml stepYaml) {
    return CollectionUtils.emptyIfNull(stepYaml.getProperties());
  }

  default void baseSetup(StepYaml stepYaml, AbstractStepNode stepNode) {
    stepNode.setIdentifier(MigratorUtility.generateIdentifier(stepYaml.getName()));
    stepNode.setName(stepYaml.getName());
    stepNode.setDescription(getDescription(stepYaml));
    if (stepNode instanceof PmsAbstractStepNode) {
      PmsAbstractStepNode pmsAbstractStepNode = (PmsAbstractStepNode) stepNode;
      pmsAbstractStepNode.setTimeout(getTimeout(stepYaml));
    }
  }
}
