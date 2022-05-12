/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.approval.step.custom.evaluation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.steps.approval.step.custom.beans.CustomApprovalTicketNG;

@OwnedBy(HarnessTeam.CDC)
public class CustomApprovalExpressionEvaluator extends EngineExpressionEvaluator {
  public static final String TICKET_IDENTIFIER = "ticket";

  private final CustomApprovalTicketNG customApprovalTicketNG;

  public CustomApprovalExpressionEvaluator(CustomApprovalTicketNG customApprovalTicketNG) {
    super(null);
    this.customApprovalTicketNG = customApprovalTicketNG;
  }

  @Override
  protected void initialize() {
    super.initialize();
    addToContext(TICKET_IDENTIFIER, customApprovalTicketNG.getFields());
  }
}
