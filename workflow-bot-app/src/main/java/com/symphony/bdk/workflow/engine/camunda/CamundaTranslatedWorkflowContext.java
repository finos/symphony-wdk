package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.workflow.engine.TranslatedWorkflowContext;
import com.symphony.bdk.workflow.engine.WorkflowDirectedGraph;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

@Getter
@EqualsAndHashCode(callSuper = true)
public class CamundaTranslatedWorkflowContext extends TranslatedWorkflowContext {
  private final BpmnModelInstance bpmnModelInstance;

  public CamundaTranslatedWorkflowContext(Workflow workflow, WorkflowDirectedGraph workflowDirectedGraph,
      BpmnModelInstance instance) {
    super(workflow, workflowDirectedGraph);
    this.bpmnModelInstance = instance;
  }
}
