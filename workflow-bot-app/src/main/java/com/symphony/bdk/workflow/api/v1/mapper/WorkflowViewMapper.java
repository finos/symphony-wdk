package com.symphony.bdk.workflow.api.v1.mapper;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.repository.ProcessDefinition;

@Slf4j
public class WorkflowViewMapper {

  private WorkflowViewMapper() {}

  public static WorkflowView toWorkflowView(ProcessDefinition processDefinition) {
    return WorkflowView.builder()
        .workflowId(processDefinition.getName())
        .build();
  }
}
