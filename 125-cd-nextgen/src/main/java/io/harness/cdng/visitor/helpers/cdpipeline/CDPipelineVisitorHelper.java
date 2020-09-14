package io.harness.cdng.visitor.helpers.cdpipeline;

import io.harness.cdng.pipeline.CDPipeline;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class CDPipelineVisitorHelper implements ConfigValidator {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Will be taken by Validation framework impl.
  }

  @Override
  public Object createDummyVisitableElement() {
    return CDPipeline.builder().build();
  }
}
