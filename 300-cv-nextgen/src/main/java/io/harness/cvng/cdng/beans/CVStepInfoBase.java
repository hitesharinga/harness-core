package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.StepSpecType;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import io.swagger.annotations.ApiModel;

@ApiModel(subTypes = {CVNGStepInfo.class})
@OwnedBy(HarnessTeam.CV)
public interface CVStepInfoBase extends StepParameters, StepSpecType {}
