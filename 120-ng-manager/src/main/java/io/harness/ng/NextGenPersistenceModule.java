package io.harness.ng;

import io.harness.NGPipelinePersistenceConfig;
import io.harness.NGTriggersPersistenceConfig;
import io.harness.OrchestrationPersistenceConfig;
import io.harness.OrchestrationStepsPersistenceConfig;
import io.harness.TimeoutEnginePersistenceConfig;
import io.harness.cdng.CDNGPersistenceConfig;
import io.harness.connector.ConnectorPersistenceConfig;
import io.harness.gitsync.GitSyncPersistenceConfig;
import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;

public class NextGenPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {TimeoutEnginePersistenceConfig.class, OrchestrationPersistenceConfig.class,
        OrchestrationStepsPersistenceConfig.class, NGPipelinePersistenceConfig.class, ConnectorPersistenceConfig.class,
        NextGenPersistenceConfig.class, GitSyncPersistenceConfig.class, CDNGPersistenceConfig.class,
        NGTriggersPersistenceConfig.class};
  }
}
