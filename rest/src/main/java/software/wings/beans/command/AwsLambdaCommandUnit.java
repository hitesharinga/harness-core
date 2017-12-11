package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.DeploymentType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

public class AwsLambdaCommandUnit extends AbstractCommandUnit {
  public AwsLambdaCommandUnit() {
    super(CommandUnitType.AWS_LAMBDA);
    setArtifactNeeded(true);
    setDeploymentType(DeploymentType.AWS_LAMBDA.name());
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    return null;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("AWS_LAMBDA")
  public static class Yaml extends AbstractCommandUnit.Yaml {
    public Yaml() {
      super();
      setCommandUnitType(CommandUnitType.AWS_LAMBDA.name());
    }

    public static final class Builder extends AbstractCommandUnit.Yaml.Builder {
      private Builder() {}

      public static Builder aYaml() {
        return new Builder();
      }

      @Override
      protected Yaml getCommandUnitYaml() {
        return new AwsLambdaCommandUnit.Yaml();
      }
    }
  }
}
