package io.harness.cdng.pipeline.service;
import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.executions.beans.dto.PipelineExecutionDTO;
import io.harness.execution.PlanExecution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

public interface NgPipelineExecutionService {
  PlanExecution triggerPipeline(
      String pipelineYaml, String accountId, String orgId, String projectId, EmbeddedUser user);

  List<PipelineExecutionDTO> getExecutions(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable);
}
