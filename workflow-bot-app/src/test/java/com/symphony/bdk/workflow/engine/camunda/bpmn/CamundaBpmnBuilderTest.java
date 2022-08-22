package com.symphony.bdk.workflow.engine.camunda.bpmn;

import com.symphony.bdk.workflow.engine.camunda.WorkflowEventToCamundaEvent;
import com.symphony.bdk.workflow.engine.camunda.bpmn.builder.WorkflowNodeBpmnBuilderFactory;

import org.camunda.bpm.engine.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CamundaBpmnBuilderTest {
  @Mock
  RepositoryService repositoryService;
  @Mock
  WorkflowEventToCamundaEvent eventToMessage;
  @Mock
  WorkflowNodeBpmnBuilderFactory builderFactory;
  @InjectMocks
  CamundaBpmnBuilder bpmnBuilder;

  @BeforeEach
  void setUp() {
  }

  @Test
  void parseWorkflowToBpmn() {
  }

  @Test
  void deployWorkflow() {
  }
}
