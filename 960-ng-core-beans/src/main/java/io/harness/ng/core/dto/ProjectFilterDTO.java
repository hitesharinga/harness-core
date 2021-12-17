package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.FilterConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "ProjectFilter", description = "This is the Project Filter defined in Harness")
public class ProjectFilterDTO {
  @Schema(description = FilterConstants.SEARCH_TERM) String searchTerm;
  @Schema(description = "Set of Organization Identifiers") Set<String> orgIdentifiers;
  @Schema(description = "This field denotes if project filter has module") Boolean hasModule;
  @Schema(description = "Module Type") ModuleType moduleType;
  @Schema(description = FilterConstants.IDENTIFIER_LIST) List<String> identifiers;
}
