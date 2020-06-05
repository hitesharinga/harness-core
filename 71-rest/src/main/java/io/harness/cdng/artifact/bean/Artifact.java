package io.harness.cdng.artifact.bean;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.SweepingOutput;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@Entity(value = "artifactsNG")
@FieldNameConstants(innerTypeName = "ArtifactsNGKeys")
@HarnessEntity(exportable = true)
public abstract class Artifact implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                          UpdatedByAware, AccountAccess, SweepingOutput {
  @Id private String uuid;
  @NotNull private String accountId;
  @NotNull private String sourceType;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @Indexed private long createdAt;

  @SchemaIgnore private EmbeddedUser lastUpdatedBy;
  @SchemaIgnore @NotNull private long lastUpdatedAt;

  @NotNull private String artifactSourceId;
}
