/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.SERVERLESS_EXECUTION_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

@OwnedBy(HarnessTeam.CDP)
public class ServerlessCommandExecutionException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public ServerlessCommandExecutionException(String message) {
    super(message, null, SERVERLESS_EXECUTION_ERROR, Level.ERROR, null, EnumSet.of(FailureType.APPLICATION_ERROR));
    super.param(MESSAGE_ARG, message);
  }

  public ServerlessCommandExecutionException(String message, FailureType failureType) {
    super(message, null, SERVERLESS_EXECUTION_ERROR, Level.ERROR, null, EnumSet.of(failureType));
    super.param(MESSAGE_ARG, message);
  }

  public ServerlessCommandExecutionException(String message, Throwable cause) {
    super(message, cause, SERVERLESS_EXECUTION_ERROR, Level.ERROR, null, EnumSet.of(FailureType.APPLICATION_ERROR));
    super.param(MESSAGE_ARG, message);
  }
}
