package com.symphony.bdk.workflow.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.workflow.engine.camunda.WorkflowEventToCamundaEvent;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowDirectGraphBuilderTest {
  @Mock
  SessionService sessionService;
  @Mock
  RuntimeService runtimeService;
  @InjectMocks
  WorkflowEventToCamundaEvent eventMapper;

  WorkflowDirectGraphBuilder workflowDirectGraphBuilder;

  @Test
  @DisplayName("Build approval workflow into a direct graph")
  void buildWorkflowDirectGraph_approvalFlow() throws Exception {
    Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/graph/approval.swadl.yaml"));
    workflowDirectGraphBuilder = new WorkflowDirectGraphBuilder(workflow, eventMapper);
    WorkflowDirectGraph directGraph = workflowDirectGraphBuilder.build();
    assertThat(directGraph.getDictionary()).hasSize(9);
    assertThat(directGraph.getStartEvents()).hasSize(1);
  }

  @Test
  @DisplayName("Build group workflow into a direct graph")
  void buildWorkflowDirectGraph_groupFlow() throws Exception {
    Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/graph/groups.swadl.yaml"));
    workflowDirectGraphBuilder = new WorkflowDirectGraphBuilder(workflow, eventMapper);
    WorkflowDirectGraph directGraph = workflowDirectGraphBuilder.build();
    assertThat(directGraph.getDictionary()).hasSize(7);
    assertThat(directGraph.getStartEvents()).hasSize(1);
  }

  @Test
  @DisplayName("Build connection workflow into a direct graph")
  void buildWorkflowDirectGraph_connectionFlow() throws Exception {
    Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/graph/connection-admin-approval.swadl.yaml"));
    workflowDirectGraphBuilder = new WorkflowDirectGraphBuilder(workflow, eventMapper);
    WorkflowDirectGraph directGraph = workflowDirectGraphBuilder.build();
    assertThat(directGraph.getDictionary()).hasSize(9);
    assertThat(directGraph.getStartEvents()).hasSize(1);
    assertThat(directGraph.getVariables()).hasSize(1);
    assertThat(directGraph.getVariables()).containsKey("administrator");
  }

  @Test
  @DisplayName("Build all of workflow into a direct graph")
  void buildWorkflowDirectGraph_allOfFlow() throws Exception {
    Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/graph/all-of.swadl.yaml"));
    workflowDirectGraphBuilder = new WorkflowDirectGraphBuilder(workflow, eventMapper);
    WorkflowDirectGraph directGraph = workflowDirectGraphBuilder.build();
    assertThat(directGraph.getDictionary()).hasSize(8);
    assertThat(directGraph.readChildren("scriptTrue").getGateway()).isEqualTo(WorkflowDirectGraph.Gateway.PARALLEL);
    assertThat(directGraph.getStartEvents()).hasSize(1);
    assertThat(directGraph.getVariables()).containsKey("allOf");
  }
}
