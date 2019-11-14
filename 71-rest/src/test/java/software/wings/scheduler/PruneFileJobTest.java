package software.wings.scheduler;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.AppContainer.Builder.anAppContainer;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scheduler.PersistentScheduler;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import software.wings.WingsBaseTest;
import software.wings.beans.AppContainer;
import software.wings.beans.Base;
import software.wings.beans.artifact.Artifact;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import java.util.Date;

public class PruneFileJobTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;

  @Mock private FileService fileService;

  @Mock private PersistentScheduler jobScheduler;

  @Inject @InjectMocks PruneFileJob job;

  private static final String ENTITY_ID = "entityId";

  public JobDetail details(String className, String entityId, FileBucket bucket) {
    return JobBuilder.newJob(PruneFileJob.class)
        .withIdentity(ENTITY_ID, PruneFileJob.GROUP)
        .usingJobData(PruneFileJob.ENTITY_CLASS_KEY, className)
        .usingJobData(PruneFileJob.ENTITY_ID_KEY, entityId)
        .usingJobData(PruneFileJob.BUCKET_KEY, bucket.name())
        .build();
  }

  public JobDetail details(Class cls, String objectId, FileBucket bucket) {
    return details(cls.getCanonicalName(), objectId, bucket);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void selfPruneTheJobWhenSucceed() throws Exception {
    when(wingsPersistence.get(Artifact.class, ENTITY_ID)).thenReturn(null);
    doNothing().when(fileService).deleteFile(ENTITY_ID, FileBucket.PLATFORMS);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Artifact.class, ENTITY_ID, FileBucket.PLATFORMS));

    job.execute(context);

    verify(wingsPersistence, times(1)).get(Artifact.class, ENTITY_ID);
    verify(fileService, times(1)).deleteAllFilesForEntity(ENTITY_ID, FileBucket.PLATFORMS);
    verify(jobScheduler, times(1)).deleteJob(ENTITY_ID, PruneFileJob.GROUP);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void selfPruneTheJobIfServiceStillThereFirstTime() throws Exception {
    when(wingsPersistence.get(AppContainer.class, ENTITY_ID)).thenReturn(anAppContainer().build());

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(AppContainer.class, ENTITY_ID, FileBucket.PLATFORMS));
    when(context.getPreviousFireTime()).thenReturn(null);

    job.execute(context);

    verify(fileService, times(0)).deleteAllFilesForEntity(ENTITY_ID, FileBucket.PLATFORMS);
    verify(jobScheduler, times(0)).deleteJob(ENTITY_ID, PruneFileJob.GROUP);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void selfPruneTheJobIfServiceStillThere() throws Exception {
    when(wingsPersistence.get(AppContainer.class, ENTITY_ID)).thenReturn(anAppContainer().build());

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(AppContainer.class, ENTITY_ID, FileBucket.PLATFORMS));
    when(context.getPreviousFireTime()).thenReturn(new Date());

    job.execute(context);

    verify(fileService, times(0)).deleteAllFilesForEntity(ENTITY_ID, FileBucket.PLATFORMS);
    verify(jobScheduler, times(1)).deleteJob(ENTITY_ID, PruneFileJob.GROUP);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void UnhandledClass() throws Exception {
    when(wingsPersistence.get(Base.class, ENTITY_ID)).thenReturn(null);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Base.class, ENTITY_ID, FileBucket.PLATFORMS));

    Logger mockLogger = mock(Logger.class);
    setStaticFieldValue(PruneFileJob.class, "logger", mockLogger);

    job.execute(context);

    verify(mockLogger, times(1)).error(any(String.class), matches(Base.class.getCanonicalName()));
    verify(fileService, times(0)).deleteAllFilesForEntity(ENTITY_ID, FileBucket.PLATFORMS);
    verify(jobScheduler, times(1)).deleteJob(ENTITY_ID, PruneFileJob.GROUP);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void WrongClass() throws Exception {
    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(Base.class, ENTITY_ID, FileBucket.PLATFORMS));

    job.execute(context);

    verify(fileService, times(0)).deleteAllFilesForEntity(ENTITY_ID, FileBucket.PLATFORMS);
    verify(jobScheduler, times(1)).deleteJob(ENTITY_ID, PruneFileJob.GROUP);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void retryIfServiceThrew() throws Exception {
    when(wingsPersistence.get(AppContainer.class, ENTITY_ID)).thenReturn(null);

    doThrow(new WingsException("Forced exception"))
        .when(fileService)
        .deleteAllFilesForEntity(ENTITY_ID, FileBucket.PLATFORMS);

    JobExecutionContext context = mock(JobExecutionContext.class);
    when(context.getJobDetail()).thenReturn(details(AppContainer.class, ENTITY_ID, FileBucket.PLATFORMS));

    job.execute(context);

    verify(jobScheduler, times(0)).deleteJob(ENTITY_ID, PruneFileJob.GROUP);
  }
}
