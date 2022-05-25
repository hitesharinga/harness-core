/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.approval.step.custom.evaluation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ApprovalStepNGException;
import io.harness.steps.approval.step.beans.ConditionDTO;
import io.harness.steps.approval.step.beans.CriteriaSpecDTO;
import io.harness.steps.approval.step.beans.JexlCriteriaSpecDTO;
import io.harness.steps.approval.step.beans.KeyValuesCriteriaSpecDTO;
import io.harness.steps.approval.step.beans.Operator;
import io.harness.steps.approval.step.custom.beans.CustomApprovalTicketNG;
import io.harness.steps.approval.step.evaluation.ConditionEvaluator;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDC)
@UtilityClass
public class CustomApprovalCriteriaEvaluator {
  public static boolean evaluateCriteria(CustomApprovalTicketNG ticket, CriteriaSpecDTO criteriaSpec) {
    if (ticket == null || ticket.getFields() == null || EmptyPredicate.isEmpty(ticket.getFields())) {
      throw new ApprovalStepNGException(
          "Custom Approval has no output fields. At least one output field must be set", true);
    }
    if (criteriaSpec instanceof JexlCriteriaSpecDTO) {
      return evaluateJexlCriteria(ticket, (JexlCriteriaSpecDTO) criteriaSpec);
    } else if (criteriaSpec instanceof KeyValuesCriteriaSpecDTO) {
      return evaluateKeyValuesCriteria(ticket, (KeyValuesCriteriaSpecDTO) criteriaSpec);
    } else {
      throw new ApprovalStepNGException("Unknown criteria type", true);
    }
  }

  private static boolean evaluateJexlCriteria(CustomApprovalTicketNG ticketNG, JexlCriteriaSpecDTO jexlCriteriaSpec) {
    String expression = jexlCriteriaSpec.getExpression();
    if (StringUtils.isBlank(expression)) {
      throw new ApprovalStepNGException("Expression cannot be blank in jexl criteria", true);
    }

    try {
      CustomApprovalExpressionEvaluator customApprovalExpressionEvaluator =
          new CustomApprovalExpressionEvaluator(ticketNG);
      Object result = customApprovalExpressionEvaluator.evaluateExpression(expression);
      if (result instanceof Boolean) {
        return (boolean) result;
      } else {
        throw new ApprovalStepNGException("Non boolean result while evaluating approval condition", true);
      }
    } catch (Exception e) {
      throw new ApprovalStepNGException(
          String.format("Error while evaluating approval condition. expression: %s%n", expression), true, e);
    }
  }

  private static boolean evaluateKeyValuesCriteria(
      CustomApprovalTicketNG ticket, KeyValuesCriteriaSpecDTO keyValueCriteriaSpec) {
    List<ConditionDTO> conditions = keyValueCriteriaSpec.getConditions();
    if (isEmpty(conditions)) {
      throw new ApprovalStepNGException("Conditions in KeyValues criteria can't be empty", true);
    }

    boolean matchAnyCondition = keyValueCriteriaSpec.isMatchAnyCondition();
    for (ConditionDTO condition : conditions) {
      try {
        Operator operator = condition.getOperator();
        String standardValue = condition.getValue();
        if (!ticket.getFields().containsKey(condition.getKey())) {
          throw new ApprovalStepNGException("Field " + condition.getKey() + " doesn't exist in ticket", true);
        }
        Object ticketValue = ticket.getFields().get(condition.getKey());
        Object ticketDisplayValue = ticket.getFields().get(condition.getKey());
        boolean currentResult;
        if (Operator.EQ.equals(operator) || Operator.IN.equals(operator)) {
          currentResult = ConditionEvaluator.evaluate(ticketValue, standardValue, operator)
              || ConditionEvaluator.evaluate(ticketDisplayValue, standardValue, operator);
        } else {
          currentResult = ConditionEvaluator.evaluate(ticketValue, standardValue, operator)
              && ConditionEvaluator.evaluate(ticketDisplayValue, standardValue, operator);
        }
        if (matchAnyCondition) {
          if (currentResult) {
            return true;
          }
        } else {
          if (!currentResult) {
            return false;
          }
        }
      } catch (Exception e) {
        throw new ApprovalStepNGException(
            String.format("Error while evaluating condition %s %s", condition.toString(), e.getMessage()), true, e);
      }
    }
    return !matchAnyCondition;
  }
}
