package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsLambdaExecuteFunctionResponse extends AwsResponse {
  private Integer statusCode;
  private String functionError;
  private String logResult;
  private String payload;

  @Builder
  public AwsLambdaExecuteFunctionResponse(ExecutionStatus executionStatus, String errorMessage, Integer statusCode,
      String functionError, String logResult, String payload) {
    super(executionStatus, errorMessage);
    this.statusCode = statusCode;
    this.functionError = functionError;
    this.logResult = logResult;
    this.payload = payload;
  }
}