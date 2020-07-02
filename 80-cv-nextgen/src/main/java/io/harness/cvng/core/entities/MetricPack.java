package io.harness.cvng.core.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.MetricPackDTO.MetricDefinitionDTO;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@CdUniqueIndex(
    name = "unique_Idx", fields = { @Field("projectIdentifier")
                                    , @Field("dataSourceType"), @Field("identifier") })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "metricPacks", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "MetricPackKeys")
@HarnessEntity(exportable = true)
public class MetricPack implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  @FdIndex private String accountId;
  @NotEmpty private String projectIdentifier;
  @NotNull private DataSourceType dataSourceType;
  @Trimmed @NotEmpty private String identifier;
  @NotEmpty private Set<MetricDefinition> metrics;
  private String dataCollectionDsl;
  @JsonIgnore
  public String getDataCollectionDsl() {
    return dataCollectionDsl;
  }

  public Set<MetricDefinition> getMetrics() {
    if (this.metrics == null) {
      return Collections.emptySet();
    }
    return metrics;
  }

  public MetricPackDTO getDTO() {
    return MetricPackDTO.builder()
        .accountId(getAccountId())
        .dataSourceType(getDataSourceType())
        .projectIdentifier(getProjectIdentifier())
        .identifier(getIdentifier())
        .metrics(getMetrics().stream().map(MetricDefinition::getDTO).collect(Collectors.toSet()))
        .build();
  }

  @Data
  @Builder
  @EqualsAndHashCode(of = {"name"})
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MetricDefinition {
    @Trimmed @NotEmpty private String name;
    @NotNull private TimeSeriesMetricType type;
    private String path;
    private String validationPath;
    private boolean included;
    @Builder.Default private List<TimeSeriesThreshold> thresholds = new ArrayList<>();
    @JsonIgnore
    public String getPath() {
      return path;
    }

    @JsonIgnore
    public String getValidationPath() {
      return validationPath;
    }

    @JsonIgnore
    public TimeSeriesMetricType getType() {
      return type;
    }

    public MetricDefinitionDTO getDTO() {
      return MetricDefinitionDTO.builder()
          .name(name)
          .path(path)
          .validationPath(validationPath)
          .included(included)
          .build();
    }
  }
}
