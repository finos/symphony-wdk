package com.symphony.bdk.workflow.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.api.v1.dto.NodeStateView;
import com.symphony.bdk.workflow.api.v1.dto.StatusEnum;
import com.symphony.bdk.workflow.api.v1.dto.VariableView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstLifeCycleFilter;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowNodesStateView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowNodesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowDirectedGraph;
import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeTypeHelper;
import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectedGraphService;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.monitoring.repository.ActivityQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.VariableQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowInstQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;
import com.symphony.bdk.workflow.swadl.v1.activity.message.SendMessage;
import com.symphony.bdk.workflow.swadl.v1.event.MessageReceivedEvent;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {
  @Mock
  WorkflowDirectedGraphService workflowDirectedGraphService;
  @Mock
  WorkflowQueryRepository workflowQueryRepository;
  @Mock
  WorkflowInstQueryRepository workflowInstQueryRepository;
  @Mock
  ActivityQueryRepository activityQueryRepository;
  @Mock
  VariableQueryRepository variableQueryRepository;
  @Mock
  ObjectConverter objectConverter;
  @InjectMocks
  MonitoringService service;

  @Test
  void listAllWorkflows() {
    service = new MonitoringService(workflowDirectedGraphService, workflowQueryRepository, workflowInstQueryRepository,
        activityQueryRepository, variableQueryRepository, objectConverter, Optional.empty());
    when(workflowQueryRepository.findAll()).thenReturn(Collections.emptyList());
    when(objectConverter.convertCollection(anyList(), eq(WorkflowView.class))).thenReturn(Collections.emptyList());
    // when
    List<WorkflowView> workflowViews = service.listAllWorkflows();
    //then
    assertThat(workflowViews).isEmpty();
  }

  @Test
  void listAllWorkflowsWithAuthor() {
    VersionedWorkflowRepository versionedWorkflowRepository = mock(VersionedWorkflowRepository.class);
    service = new MonitoringService(workflowDirectedGraphService, workflowQueryRepository, workflowInstQueryRepository,
        activityQueryRepository, variableQueryRepository, objectConverter, Optional.of(versionedWorkflowRepository));
    when(versionedWorkflowRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
    when(objectConverter.convertCollection(anyList(), eq(WorkflowView.class))).thenReturn(Collections.emptyList());
    // when
    List<WorkflowView> workflowViews = service.listAllWorkflows();
    //then
    assertThat(workflowViews).isEmpty();
  }

  @Test
  void listWorkflowInstances() {
    when(workflowInstQueryRepository.findAllById(anyString())).thenReturn(Collections.emptyList());
    when(objectConverter.convertCollection(anyList(), eq(WorkflowInstView.class))).thenReturn(Collections.emptyList());
    // when
    List<WorkflowInstView> workflowViews = service.listWorkflowInstances("id", null, null);
    //then
    assertThat(workflowViews).isEmpty();
    verify(workflowInstQueryRepository).findAllById(anyString());
  }

  @ParameterizedTest
  @CsvSource({"completed", "COMPLETED", "pending", "PENDING", "active", "FAILED", "failed"})
  void listWorkflowInstances_completedFilter(String status) {
    when(workflowInstQueryRepository.findAllByIdAndStatus(anyString(), any(StatusEnum.class))).thenReturn(
        Collections.emptyList());
    when(objectConverter.convertCollection(anyList(), eq(WorkflowInstView.class))).thenReturn(Collections.emptyList());
    // when
    List<WorkflowInstView> workflowViews = service.listWorkflowInstances("id", status, null);
    //then
    assertThat(workflowViews).isEmpty();
    verify(workflowInstQueryRepository).findAllByIdAndStatus(anyString(), any(StatusEnum.class));
  }

  @Test
  void listWorkflowInstancesWithVersion() {
    when(workflowInstQueryRepository.findAllByIdAndVersion(anyString(), anyString())).thenReturn(
        Collections.emptyList());
    when(objectConverter.convertCollection(anyList(), eq(WorkflowInstView.class))).thenReturn(Collections.emptyList());
    // when
    List<WorkflowInstView> workflowViews = service.listWorkflowInstances("id", null, 1234L);
    //then
    assertThat(workflowViews).isEmpty();
    verify(workflowInstQueryRepository).findAllByIdAndVersion(anyString(), anyString());
  }

  @Test
  void listWorkflowInstancesWithStatusAndVersion() {
    when(workflowInstQueryRepository.findAllByIdAndStatusAndVersion(anyString(), eq(StatusEnum.PENDING),
        anyString())).thenReturn(Collections.emptyList());
    when(objectConverter.convertCollection(anyList(), eq(WorkflowInstView.class))).thenReturn(Collections.emptyList());
    // when
    List<WorkflowInstView> workflowViews = service.listWorkflowInstances("id", "active", 1234L);
    //then
    assertThat(workflowViews).isEmpty();
    verify(workflowInstQueryRepository).findAllByIdAndStatusAndVersion(anyString(), eq(StatusEnum.PENDING),
        anyString());
  }

  @Test
  void listWorkflowInstancesBadStatus() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .as("bad_status is not a valid workflow instance status")
        .isThrownBy(() -> service.listWorkflowInstances("id", "bad_status", null))
        .satisfies(exception -> assertThat(exception.getMessage()).isEqualTo(
            "Workflow instance status bad_status is not known. Allowed values [Completed, Pending, Failed]"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "errors"})
  void listWorkflowInstanceActivities(String errors) {
    // given
    NodeStateView view1 = NodeStateView.builder()
        .instanceId("instance")
        .nodeId("activity1")
        .workflowId("workflow")
        .endDate(Instant.now())
        .startDate(Instant.now())
        .duration(Duration.ofMillis(2000))
        .type(WorkflowNodeTypeHelper.toType("MESSAGE_RECEIVED_EVENT"))
        .group(WorkflowNodeTypeHelper.toGroup("MESSAGE_RECEIVED_EVENT"))
        .outputs(Maps.newHashMap("key", "value1")).build();
    NodeStateView view2 = NodeStateView.builder()
        .instanceId("instance")
        .nodeId("activity2")
        .workflowId("workflow")
        .endDate(Instant.now())
        .startDate(Instant.now())
        .duration(Duration.ofMillis(2000))
        .type(WorkflowNodeTypeHelper.toType("MESSAGE_RECEIVED_EVENT"))
        .group(WorkflowNodeTypeHelper.toGroup("MESSAGE_RECEIVED_EVENT"))
        .outputs(Maps.newHashMap("key", "value2")).build();

    WorkflowInstanceDomain workflowInstanceDomain = WorkflowInstanceDomain.builder().instanceId("instance").build();
    WorkflowInstView workflowInstView = WorkflowInstView.builder().id("workflow").instanceId("instance").build();
    when(workflowInstQueryRepository.findAllById("workflow")).thenReturn(
        Collections.singletonList(workflowInstanceDomain));
    when(activityQueryRepository.findAllByWorkflowInstanceId(anyString(), anyString(),
        any(WorkflowInstLifeCycleFilter.class))).thenReturn(Collections.singletonList(ActivityInstanceDomain.builder()
        .build())); // returns at least one item, otherwise an IllegalArgumentException will be thrown
    when(objectConverter.convertCollection(anyList(), eq(NodeStateView.class))).thenReturn(
        List.of(view1, view2));
    when(objectConverter.convertCollection(anyList(), eq(WorkflowInstView.class))).thenReturn(
        List.of(workflowInstView));

    // mock graph
    WorkflowNode activity1 = new WorkflowNode();
    SendMessage sendMessage = new SendMessage();
    sendMessage.setContent("content");
    sendMessage.setId("activity1");
    activity1.activity(sendMessage);
    activity1.id("activity1");
    activity1.eventId("activity1");
    activity1.setWrappedType(MessageReceivedEvent.class);

    WorkflowNode activity2 = new WorkflowNode();
    sendMessage.setContent("content");
    sendMessage.setId("activity1");
    activity2.activity(sendMessage);
    activity2.id("activity2");
    activity2.eventId("activity2");
    activity2.setWrappedType(MessageReceivedEvent.class);

    WorkflowDirectedGraph directGraph = new WorkflowDirectedGraph("workflow");
    directGraph.registerToDictionary("activity1", activity1);
    directGraph.registerToDictionary("activity2", activity2);

    when(workflowDirectedGraphService.getDirectedGraph("workflow")).thenReturn(directGraph);

    // mock variables
    VariablesDomain vars = new VariablesDomain();
    vars.setRevision(2);
    vars.setUpdateTime(Instant.now());
    vars.setOutputs(Maps.newHashMap("key", "value"));
    when(variableQueryRepository.findVarsByWorkflowInstanceIdAndVarName(anyString(), eq("variables"))).thenReturn(vars);
    if ("errors".equals(errors)) {
      VariablesDomain err = new VariablesDomain();
      err.setRevision(0);
      err.setUpdateTime(Instant.now());
      err.setOutputs(Maps.newHashMap("error", "exception"));
      when(variableQueryRepository.findVarsByWorkflowInstanceIdAndVarName(anyString(), eq("error"))).thenReturn(err);
    } else {
      when(variableQueryRepository.findVarsByWorkflowInstanceIdAndVarName(anyString(), eq("error"))).thenReturn(
          new VariablesDomain());
    }

    // when
    WorkflowNodesStateView workflowInstanceActivities = service.listWorkflowInstanceNodes("workflow", "instance",
        new WorkflowInstLifeCycleFilter(null, null, null, null));

    // then
    assertThat(workflowInstanceActivities.getNodes()).hasSize(2);
    assertThat(workflowInstanceActivities.getNodes().get(0).getOutputs()).hasSize(1);
    assertThat(workflowInstanceActivities.getNodes().get(0).getWorkflowId()).isEqualTo("workflow");
    assertThat(workflowInstanceActivities.getNodes().get(0).getInstanceId()).isEqualTo("instance");
    assertThat(workflowInstanceActivities.getNodes().get(0).getNodeId()).isEqualTo("activity1");
    assertThat(workflowInstanceActivities.getNodes().get(0).getStartDate()).isNotNull();
    assertThat(workflowInstanceActivities.getNodes().get(0).getEndDate()).isNotNull();
    assertThat(workflowInstanceActivities.getGlobalVariables().getRevision()).isEqualTo(2);
    assertThat(workflowInstanceActivities.getGlobalVariables().getUpdateTime()).isNotNull();
    if ("errors".equals(errors)) {
      assertThat(workflowInstanceActivities.getError()).hasSize(1);
    } else {
      assertThat(workflowInstanceActivities.getError()).isNull();
    }
  }

  @Test
  void listWorkflowInstanceActivities_badInstanceId_illegalArgumentException() {
    // given
    when(workflowInstQueryRepository.findAllById("workflow")).thenReturn(Collections.emptyList());
    when(objectConverter.convertCollection(anyList(), eq(WorkflowInstView.class))).thenReturn(Collections.emptyList());

    // when
    WorkflowInstLifeCycleFilter lifeCycleFilter = new WorkflowInstLifeCycleFilter(null, null, null, null);
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
            () -> service.listWorkflowInstanceNodes("workflow", "instance", lifeCycleFilter))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(
            "Either no workflow deployed with id workflow, or instance is not an instance of it"));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(longs = {1234L})
  void getWorkflowDefinition(Long version) {
    // mock graph
    WorkflowNode activity1 = new WorkflowNode();
    SendMessage sendMessage = new SendMessage();
    sendMessage.setContent("content");
    sendMessage.setId("activity1");
    activity1.activity(sendMessage);
    activity1.id("activity1");
    activity1.wrappedType(SendMessage.class);

    WorkflowNode activity2 = new WorkflowNode();
    sendMessage.setContent("content");
    sendMessage.setId("activity1");
    activity2.activity(sendMessage);
    activity2.id("activity2");
    activity2.wrappedType(SendMessage.class);

    WorkflowDirectedGraph directedGraph = new WorkflowDirectedGraph("workflow", version);
    directedGraph.registerToDictionary("activity1", activity1);
    directedGraph.registerToDictionary("activity2", activity2);
    directedGraph.addParent("activity2", "activity1");
    directedGraph.getChildren("activity1").addChild("activity2");
    directedGraph.getVariables().put("variable", "value");

    WorkflowNodesView definitionView;
    if (Optional.ofNullable(version).isPresent()) {
      when(workflowDirectedGraphService.getDirectedGraph(eq("workflow"), eq(version))).thenReturn(directedGraph);
      definitionView = service.getWorkflowDefinition("workflow", version);
    } else {
      when(workflowDirectedGraphService.getDirectedGraph(eq("workflow"))).thenReturn(directedGraph);
      definitionView = service.getWorkflowDefinition("workflow");
    }

    // then
    assertThat(definitionView.getWorkflowId()).isEqualTo("workflow");
    assertThat(definitionView.getVersion()).isEqualTo(version);
    assertThat(definitionView.getVariables()).hasSize(1);
    assertThat(definitionView.getVariables()).containsKey("variable");
    assertThat(definitionView.getFlowNodes()).hasSize(2);
    assertThat(definitionView.getFlowNodes().get(0).getChildren()).hasSize(1);
    assertThat(definitionView.getFlowNodes().get(1).getParents()).hasSize(1);
    assertThat(definitionView.getFlowNodes().get(0).getType()).isEqualTo("SEND_MESSAGE");
    assertThat(definitionView.getFlowNodes().get(0).getGroup()).isEqualTo("ACTIVITY");
  }

  @Test
  void listWorkflowInstanceGlobalVars() {
    // mock graph
    VariablesDomain domain = new VariablesDomain();
    domain.setUpdateTime(Instant.now());
    domain.setRevision(1);
    domain.setOutputs(Maps.newHashMap("key", "value"));

    WorkflowInstanceDomain workflowInstanceDomain = WorkflowInstanceDomain.builder().instanceId("instance").build();
    WorkflowInstView workflowInstView = WorkflowInstView.builder().id("workflow").instanceId("instance").build();
    when(workflowInstQueryRepository.findAllById("workflow")).thenReturn(
        Collections.singletonList(workflowInstanceDomain));
    when(objectConverter.convertCollection(anyList(), eq(WorkflowInstView.class))).thenReturn(
        List.of(workflowInstView));
    when(variableQueryRepository.findGlobalVarsHistoryByWorkflowInstId(anyString(), any(), any())).thenReturn(
        List.of(domain));

    // when
    List<VariableView> variableViews = service.listWorkflowInstanceGlobalVars("workflow", "instance", null, null);

    // then
    assertThat(variableViews).hasSize(1);
    assertThat(variableViews.get(0).getUpdateTime()).isNotNull();
    assertThat(variableViews.get(0).getRevision()).isEqualTo(1);
    assertThat(variableViews.get(0).getOutputs()).hasSize(1);
  }
}
