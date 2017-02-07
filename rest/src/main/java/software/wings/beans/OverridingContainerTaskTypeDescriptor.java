package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;

import java.util.Optional;

/**
 * Created by anubhaw on 2/6/17.
 */
public class OverridingContainerTaskTypeDescriptor
    implements OverridingStencil<ContainerTask>, ContainerTaskTypeDescriptor {
  private ContainerTaskTypeDescriptor containerTaskTypeDescriptor;
  private Optional<String> overridingName = Optional.empty();
  private Optional<JsonNode> overridingJsonSchema = Optional.empty();

  public OverridingContainerTaskTypeDescriptor(ContainerTaskTypeDescriptor containerTaskTypeDescriptor) {
    this.containerTaskTypeDescriptor = containerTaskTypeDescriptor;
  }

  @Override
  public String getType() {
    return containerTaskTypeDescriptor.getType();
  }

  @Override
  @JsonIgnore
  public Class<? extends ContainerTask> getTypeClass() {
    return containerTaskTypeDescriptor.getTypeClass();
  }

  @Override
  public JsonNode getJsonSchema() {
    return overridingJsonSchema.isPresent() ? overridingJsonSchema.get().deepCopy()
                                            : containerTaskTypeDescriptor.getJsonSchema();
  }

  @Override
  public Object getUiSchema() {
    return containerTaskTypeDescriptor.getUiSchema();
  }

  @Override
  public String getName() {
    return overridingName.orElse(containerTaskTypeDescriptor.getName());
  }

  @Override
  public OverridingStencil getOverridingStencil() {
    return containerTaskTypeDescriptor.getOverridingStencil();
  }

  @Override
  public ContainerTask newInstance(String id) {
    return containerTaskTypeDescriptor.newInstance(id);
  }

  @Override
  public JsonNode getOverridingJsonSchema() {
    return overridingJsonSchema.orElse(null);
  }

  @Override
  public void setOverridingJsonSchema(JsonNode overridingJsonSchema) {
    this.overridingJsonSchema = Optional.ofNullable(overridingJsonSchema);
  }

  @Override
  public String getOverridingName() {
    return overridingName.orElse(null);
  }

  @Override
  public void setOverridingName(String overridingName) {
    this.overridingName = Optional.ofNullable(overridingName);
  }

  @Override
  public StencilCategory getStencilCategory() {
    return containerTaskTypeDescriptor == null ? null : containerTaskTypeDescriptor.getStencilCategory();
  }

  @Override
  public Integer getDisplayOrder() {
    return containerTaskTypeDescriptor == null ? DEFAULT_DISPLAY_ORDER : containerTaskTypeDescriptor.getDisplayOrder();
  }
}
