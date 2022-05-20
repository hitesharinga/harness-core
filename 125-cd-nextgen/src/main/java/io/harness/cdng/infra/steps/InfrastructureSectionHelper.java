/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.infra.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnsupportedOperationException;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.logstreaming.NGLogCallback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.execution.utils.AmbianceUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class InfrastructureSectionHelper {
  private static String INFRASTRUCTURE_COMMAND_UNIT = "Execute";

  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;

  public NGLogCallback getServiceLogCallback(Ambiance ambiance) {
    return getServiceLogCallback(ambiance, false);
  }

  public NGLogCallback getServiceLogCallback(Ambiance ambiance, boolean shouldOpenStream) {
    return new NGLogCallback(logStreamingStepClientFactory, prepareInfrastructureAmbiance(ambiance),
        INFRASTRUCTURE_COMMAND_UNIT, shouldOpenStream);
  }

  private Ambiance prepareInfrastructureAmbiance(Ambiance ambiance) {
    List<Level> levels = ambiance.getLevelsList();
    for (int i = levels.size() - 1; i >= 0; i--) {
      Level level = levels.get(i);
      if (InfrastructureStep.STEP_TYPE.equals(level.getStepType())) {
        return AmbianceUtils.clone(ambiance, i + 1);
      }
    }
    throw new UnsupportedOperationException("Not inside infrastructure step or one of it's children");
  }
}
