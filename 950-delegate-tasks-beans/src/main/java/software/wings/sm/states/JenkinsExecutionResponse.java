package software.wings.sm.states;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import software.wings.beans.JenkinsSubTaskType;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public final class JenkinsExecutionResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String jenkinsResult;
  private String errorMessage;
  private String jobUrl;
  private List<FilePathAssertionEntry> filePathAssertionMap = Lists.newArrayList();
  private String buildNumber;
  private Map<String, String> metadata;
  private Map<String, String> jobParameters;
  private Map<String, String> envVars;
  private String description;
  private String buildDisplayName;
  private String buildFullDisplayName;
  private String queuedBuildUrl;
  private JenkinsSubTaskType subTaskType;
  private String activityId;
  private Long timeElapsed; // time taken for task completion
}
