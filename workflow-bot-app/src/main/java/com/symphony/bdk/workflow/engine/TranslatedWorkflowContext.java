package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TranslatedWorkflowContext {
  private final Workflow workflow;
  private final WorkflowDirectedGraph workflowDirectedGraph;
}
