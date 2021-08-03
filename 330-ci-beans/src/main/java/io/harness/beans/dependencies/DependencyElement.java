package io.harness.beans.dependencies;

import static io.harness.annotations.dev.HarnessTeam.CI;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.beans.SwaggerConstants;
import io.harness.beans.WithIdentifier;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("dependency")
@OwnedBy(CI)
public class DependencyElement implements WithIdentifier {
  @EntityIdentifier String identifier;
  @EntityName String name;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> description;

  @NotNull @ApiModelProperty(allowableValues = DependencyConstants.SERVICE_TYPE) DependencyType type;

  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  DependencySpecType dependencySpecType;

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDependencySpecType(DependencySpecType dependencySpecType) {
    this.dependencySpecType = dependencySpecType;
    if (this.dependencySpecType != null) {
      this.dependencySpecType.setIdentifier(identifier);
      this.dependencySpecType.setName(name);
    }
  }

  @Builder
  public DependencyElement(String identifier, String name, DependencyType type, DependencySpecType dependencySpecType) {
    this.identifier = identifier;
    this.name = name;
    this.type = type;
    this.dependencySpecType = dependencySpecType;
  }
}
