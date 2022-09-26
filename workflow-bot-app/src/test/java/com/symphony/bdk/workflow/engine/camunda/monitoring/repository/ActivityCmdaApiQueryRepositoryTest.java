package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstLifeCycleFilter;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.history.NativeHistoricVariableInstanceQuery;
import org.camunda.community.mockito.QueryMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ActivityCmdaApiQueryRepositoryTest {
  @Mock HistoryService historyService;
  @Mock ObjectConverter objectConverter;
  @InjectMocks ActivityCmdaApiQueryRepository queryRepository;

  @BeforeEach
  void setUp() {
    HistoricActivityInstance instance1 = mock(HistoricActivityInstance.class);
    HistoricActivityInstance instance2 = mock(HistoricActivityInstance.class);
    QueryMocks.mockHistoricActivityInstanceQuery(historyService).list(List.of(instance1, instance2));
  }

  @Test
  void findAllByWorkflowInstanceId_hasVariables() {
    ActivityInstanceDomain domain1 = ActivityInstanceDomain.builder()
        .type("serviceTask")
        .name("instance1")
        .workflowId("wf")
        .procInstId("inst")
        .id("id1")
        .build();
    ActivityInstanceDomain domain2 = ActivityInstanceDomain.builder()
        .type("serviceTask")
        .name("instance2")
        .workflowId("wf")
        .procInstId("inst")
        .id("id2")
        .build();
    when(objectConverter.convertCollection(anyList(), eq(ActivityInstanceDomain.class))).thenReturn(
        List.of(domain1, domain2));

    NativeHistoricVariableInstanceQuery query = mock(NativeHistoricVariableInstanceQuery.class);
    when(query.sql(contains("instance1"))).thenReturn(query);

    HistoricVariableInstance inst1 = mock(HistoricVariableInstance.class);
    HistoricVariableInstance inst2 = mock(HistoricVariableInstance.class);
    when(query.list()).thenReturn(List.of(inst1, inst2));
    when(historyService.createNativeHistoricVariableInstanceQuery()).thenReturn(query);
    when(inst1.getName()).thenReturn("instance1");

    Map<String, String> vars = new HashMap<>();
    vars.put("key1", "value1");
    when(inst1.getValue()).thenReturn(Collections.singletonMap("outputs", vars));
    when(inst1.getCreateTime()).thenReturn(new Date());
    when(inst2.getName()).thenReturn("instance2");

    Map<String, String> vars2 = new HashMap<>();
    vars2.put("key2", "value2");
    when(inst2.getValue()).thenReturn(Collections.singletonMap("outputs", vars2));
    when(inst2.getCreateTime()).thenReturn(new Date());

    List<ActivityInstanceDomain> result = queryRepository.findAllByWorkflowInstanceId("wf", "inst",
        new WorkflowInstLifeCycleFilter(null, null, null, null));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getId()).isEqualTo("id1");
    assertThat(result.get(0).getName()).isEqualTo("instance1");
    assertThat(result.get(0).getVariables().getOutputs()).hasSize(1);
  }

  @Test
  void findAllByWorkflowInstanceId_hasNotVariables() {
    ActivityInstanceDomain domain1 = ActivityInstanceDomain.builder()
        .type("scriptTask")
        .name("instance1")
        .workflowId("wf")
        .procInstId("inst")
        .id("id1")
        .build();
    ActivityInstanceDomain domain2 = ActivityInstanceDomain.builder()
        .type("scriptTask")
        .name("instance2")
        .workflowId("wf")
        .procInstId("inst")
        .id("id2")
        .build();
    when(objectConverter.convertCollection(anyList(), eq(ActivityInstanceDomain.class))).thenReturn(
        List.of(domain1, domain2));

    List<ActivityInstanceDomain> result = queryRepository.findAllByWorkflowInstanceId("wf", "inst",
        new WorkflowInstLifeCycleFilter(null, null, null, null));
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getId()).isEqualTo("id1");
    assertThat(result.get(0).getName()).isEqualTo("instance1");
    assertThat(result.get(0).getVariables().getOutputs()).isEmpty();
  }
}
