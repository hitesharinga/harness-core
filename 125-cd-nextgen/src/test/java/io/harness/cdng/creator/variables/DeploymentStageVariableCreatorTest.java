/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.variables;

import static io.harness.rule.OwnerRule.NAMAN_TALAYCHA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.creator.plan.stage.DeploymentStageNode;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.variables.beans.VariableCreationContext;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DeploymentStageVariableCreatorTest extends CategoryTest {
  DeploymentStageVariableCreator deploymentStageVariableCreator = new DeploymentStageVariableCreator();
  private static final String STAGE_ID = "NnmWEe_TRXCba1-R2EsDrw";
  private static final String SERVICE_CONFIG_ID = "7Oszx9TqQddNkz3_emc5ng";
  private static final String INFRASTRUCTURE_ID = "KmxClH1bSKiNstn52Cf6BA";
  private static final String EXECUTION_ID = "NnmWEe_TRXCba1-R2Esssw";

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testCreateVariablesForChildrenNodesV2() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("deployment_stage.json");
    String json = Resources.toString(testFile, Charsets.UTF_8);
    JsonNode jsonNode = JsonUtils.asObject(json, JsonNode.class);
    YamlNode deploymentYamlNode = new YamlNode("stage", jsonNode);
    YamlField yamlField = new YamlField(deploymentYamlNode);
    LinkedHashMap<String, VariableCreationResponse> variablesMap =
        deploymentStageVariableCreator.createVariablesForChildrenNodesV2(
            VariableCreationContext.builder().currentField(yamlField).build(),
            YamlUtils.read(yamlField.getNode().toString(), DeploymentStageNode.class));
    for (Map.Entry<String, VariableCreationResponse> entry : variablesMap.entrySet()) {
      List<String> fqnPropertiesList = entry.getValue()
                                           .getYamlProperties()
                                           .values()
                                           .stream()
                                           .map(YamlProperties::getFqn)
                                           .collect(Collectors.toList());

      if (SERVICE_CONFIG_ID.equals(entry.getKey())) {
        assertThat(fqnPropertiesList)
            .containsAll(Arrays.asList("serviceConfig", "spec.serviceConfig.service.identifier",
                "spec.serviceConfig.serviceDefinition.spec.artifacts.primary.spec.connectorRef",
                "spec.serviceConfig.serviceDefinition.spec.artifacts.primary.spec.imagePath",
                "spec.serviceConfig.serviceDefinition.spec.artifacts.primary.spec.tag",
                "spec.serviceConfig.serviceDefinition.spec.manifests.baseValues.spec.store.spec.connectorRef",
                "spec.serviceConfig.serviceDefinition.spec.manifests.baseValues.spec.store.spec.gitFetchType",
                "spec.serviceConfig.serviceDefinition.spec.manifests.baseValues.spec.store.spec.branch",
                "spec.serviceConfig.serviceDefinition.spec.manifests.baseValues.spec.store.spec.paths"));
      } else if (INFRASTRUCTURE_ID.equals(entry.getKey())) {
        assertThat(fqnPropertiesList)
            .containsAll(
                Arrays.asList("infrastructure", "spec.infrastructure.infrastructureDefinition.spec.connectorRef",
                    "spec.infrastructure.infrastructureDefinition.spec.namespace",
                    "spec.infrastructure.infrastructureDefinition.spec.releaseName",
                    "spec.infrastructure.environment.tags.cloud", "spec.infrastructure.environment.tags.team"));
      }
    }

    assertThat(variablesMap.get(STAGE_ID)).isNotNull();
    String yamlPath = variablesMap.get(STAGE_ID).getDependencies().getDependenciesMap().get(STAGE_ID);
    YamlField fullYamlField = YamlUtils.readTree(json);
    assertThat(fullYamlField).isNotNull();
    YamlField specYaml = fullYamlField.fromYamlPath(yamlPath);
    assertThat(yamlField.getNode().getFieldName()).isNotEmpty();
    assertThat(specYaml.getName()).isEqualTo("execution");
    assertThat(specYaml.getNode().fetchKeys()).containsExactlyInAnyOrder("steps", "rollbackSteps", "__uuid");
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void getSupportedTypes() {
    assertThat(deploymentStageVariableCreator.getSupportedTypes())
        .containsEntry(YAMLFieldNameConstants.STAGE, Collections.singleton("Deployment"));
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void getClassType() {
    assertThat(deploymentStageVariableCreator.getFieldClass()).isEqualTo(DeploymentStageNode.class);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testCreateVariablesForParentNodes() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipelineVariableCreatorUuidJson.yaml");
    String pipelineJson = Resources.toString(testFile, Charsets.UTF_8);
    YamlField fullYamlField = YamlUtils.readTree(pipelineJson);

    // Pipeline Node
    YamlField stageField = fullYamlField.getNode()
                               .getField("pipeline")
                               .getNode()
                               .getField("stages")
                               .getNode()
                               .asArray()
                               .get(0)
                               .getField("stage");
    // yaml input expressions
    VariableCreationResponse variablesForParentNodeV2 = deploymentStageVariableCreator.createVariablesForParentNodeV2(
        VariableCreationContext.builder().currentField(stageField).build(),
        YamlUtils.read(stageField.getNode().toString(), DeploymentStageNode.class));
    List<String> fqnPropertiesList = variablesForParentNodeV2.getYamlProperties()
                                         .values()
                                         .stream()
                                         .map(YamlProperties::getFqn)
                                         .collect(Collectors.toList());
    assertThat(fqnPropertiesList)
        .containsAll(Arrays.asList("pipeline.stages.qaStage.description", "pipeline.stages.qaStage.name"));

    // yaml extra properties
    List<String> fqnExtraPropertiesList = variablesForParentNodeV2.getYamlExtraProperties()
                                              .get("tGufMZnYTNCcFFLz74wtpA") // pipeline uuid
                                              .getPropertiesList()
                                              .stream()
                                              .map(YamlProperties::getFqn)
                                              .collect(Collectors.toList());
    assertThat(fqnExtraPropertiesList)
        .containsAll(Arrays.asList("pipeline.stages.qaStage.variables", "pipeline.stages.qaStage.identifier",
            "pipeline.stages.qaStage.startTs", "pipeline.stages.qaStage.endTs", "pipeline.stages.qaStage.tags",
            "pipeline.stages.qaStage.type"));
  }
}
