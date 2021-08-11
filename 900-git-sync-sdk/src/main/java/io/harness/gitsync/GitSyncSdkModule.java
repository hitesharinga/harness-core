package io.harness.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_CONFIG_STREAM;
import static io.harness.gitsync.AbstractGitSyncSdkModule.GIT_SYNC_SDK;

import io.harness.SCMJavaClientModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.dao.GitProcessingRequestService;
import io.harness.gitsync.dao.GitProcessingRequestServiceImpl;
import io.harness.gitsync.events.GitSyncConfigEventMessageListener;
import io.harness.gitsync.fullsync.FullSyncSdkService;
import io.harness.gitsync.fullsync.FullSyncSdkServiceImpl;
import io.harness.gitsync.gittoharness.ChangeSetHelperServiceImpl;
import io.harness.gitsync.gittoharness.ChangeSetInterceptorService;
import io.harness.gitsync.gittoharness.GitSdkInterface;
import io.harness.gitsync.gittoharness.GitToHarnessSdkProcessor;
import io.harness.gitsync.gittoharness.GitToHarnessSdkProcessorImpl;
import io.harness.gitsync.gittoharness.NoOpChangeSetInterceptorServiceImpl;
import io.harness.gitsync.persistance.EntityKeySource;
import io.harness.gitsync.persistance.EntityLookupHelper;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitAwarePersistenceNewImpl;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.gitsync.persistance.GitSyncSdkServiceImpl;
import io.harness.gitsync.scm.ScmDelegateGitHelper;
import io.harness.gitsync.scm.ScmGitHelper;
import io.harness.gitsync.scm.ScmManagerGitHelper;
import io.harness.gitsync.sdk.GitSyncGrpcClientModule;
import io.harness.gitsync.sdk.GitSyncSdkGrpcServerModule;
import io.harness.ng.core.event.MessageListener;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

@OwnedBy(DX)
public class GitSyncSdkModule extends AbstractModule {
  public static final String SCM_ON_DELEGATE = "scmOnDelegate";
  public static final String SCM_ON_MANAGER = "scmOnManager";

  private static volatile GitSyncSdkModule instance;

  static GitSyncSdkModule getInstance() {
    if (instance == null) {
      instance = new GitSyncSdkModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(GitSyncGrpcClientModule.getInstance());
    install(GitSyncSdkGrpcServerModule.getInstance());
    install(SCMJavaClientModule.getInstance());
    //    bind(new TypeLiteral<GitAwareRepository<?, ?, ?>>() {}).to(new TypeLiteral<GitAwareRepositoryImpl<?, ?, ?>>()
    //    {});
    bind(GitToHarnessSdkProcessor.class).to(GitToHarnessSdkProcessorImpl.class);
    bind(ChangeSetInterceptorService.class).to(NoOpChangeSetInterceptorServiceImpl.class);
    bind(EntityKeySource.class).to(EntityLookupHelper.class);
    bind(GitSdkInterface.class).to(ChangeSetHelperServiceImpl.class);
    bind(MessageListener.class)
        .annotatedWith(Names.named(GIT_CONFIG_STREAM + GIT_SYNC_SDK))
        .to(GitSyncConfigEventMessageListener.class);
    bind(GitAwarePersistence.class).to(GitAwarePersistenceNewImpl.class);
    bind(ScmGitHelper.class).annotatedWith(Names.named(SCM_ON_MANAGER)).to(ScmManagerGitHelper.class);
    bind(ScmGitHelper.class).annotatedWith(Names.named(SCM_ON_DELEGATE)).to(ScmDelegateGitHelper.class);
    bind(GitSyncSdkService.class).to(GitSyncSdkServiceImpl.class);
    bind(GitProcessingRequestService.class).to(GitProcessingRequestServiceImpl.class);
    bind(FullSyncSdkService.class).to(FullSyncSdkServiceImpl.class);
  }
}
