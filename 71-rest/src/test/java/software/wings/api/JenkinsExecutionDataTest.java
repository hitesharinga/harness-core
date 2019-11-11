package software.wings.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class JenkinsExecutionDataTest extends CategoryTest {
  private JenkinsExecutionData jenkinsExecutionData =
      JenkinsExecutionData.builder().jobName("testjob").buildUrl("http://jenkins/testjob/11").build();

  @Before
  public void setup() {
    jenkinsExecutionData.setErrorMsg("Err");
    jenkinsExecutionData.setJobStatus("ERROR");
    jenkinsExecutionData.setStatus(ExecutionStatus.FAILED);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetExecutionSummary() {
    assertThat(jenkinsExecutionData.getExecutionSummary())
        .containsAllEntriesOf(ImmutableMap.of("jobName",
            ExecutionDataValue.builder().displayName("Job Name").value("testjob").build(), "build",
            ExecutionDataValue.builder().displayName("Build Url").value("http://jenkins/testjob/11").build(),
            "jobStatus", ExecutionDataValue.builder().displayName("Job Status").value("ERROR").build()));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetExecutionDetails() {
    assertThat(jenkinsExecutionData.getExecutionDetails())
        .containsAllEntriesOf(ImmutableMap.of("jobName",
            ExecutionDataValue.builder().displayName("Job Name").value("testjob").build(), "build",
            ExecutionDataValue.builder().displayName("Build Url").value("http://jenkins/testjob/11").build(),
            "jobStatus", ExecutionDataValue.builder().displayName("Job Status").value("ERROR").build()));
  }
}
