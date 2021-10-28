package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.DEL)
public class DelegateSetupDetails implements YamlDTO {
  private String orgIdentifier;
  private String projectIdentifier;
  @NotNull private String name;
  private String description;
  private DelegateSize size;
  // TODO: Remove delegateCongigId once we drop this from UI.
  private String delegateConfigurationId;
  // This can be blank also, since we can create a group from delegate yaml itself.
  @EntityIdentifier(allowBlank = true) private String identifier;

  private K8sConfigDetails k8sConfigDetails;

  private Set<String> tags;
}
