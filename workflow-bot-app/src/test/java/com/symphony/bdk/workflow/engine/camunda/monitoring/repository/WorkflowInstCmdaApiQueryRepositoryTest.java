package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import com.symphony.bdk.workflow.api.v1.dto.StatusEnum;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.community.mockito.QueryMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowInstCmdaApiQueryRepositoryTest {
  @Mock HistoryService historyService;
  @Mock RepositoryService repositoryService;
  @Mock ObjectConverter objectConverter;
  @InjectMocks WorkflowInstCmdaApiQueryRepository queryRepository;

  @Test
  void findAllById() {
    // given
    HistoricProcessInstance instance1 = mock(HistoricProcessInstance.class);
    HistoricProcessInstance instance2 = mock(HistoricProcessInstance.class);
    QueryMocks.mockHistoricProcessInstanceQuery(historyService).list(List.of(instance1, instance2));
    ProcessDefinition definition1 = mock(ProcessDefinition.class);
    ProcessDefinition definition2 = mock(ProcessDefinition.class);
    QueryMocks.mockProcessDefinitionQuery(repositoryService).list(List.of(definition1, definition2));

    WorkflowInstanceDomain domain1 = WorkflowInstanceDomain.builder()
        .name("workflow")
        .status("active")
        .version(1L)
        .startDate(Instant.now())
        .endDate(Instant.now())
        .instanceId("inst-id1")
        .duration(Duration.ofMillis(5000))
        .id("id1")
        .build();
    WorkflowInstanceDomain domain2 = WorkflowInstanceDomain.builder()
        .name("workflow")
        .status("pending")
        .version(1L)
        .startDate(Instant.now())
        .instanceId("inst-id2")
        .id("id2")
        .build();
    when(objectConverter.convertCollection(anyList(), eq(WorkflowInstanceDomain.class))).thenReturn(
        List.of(domain1, domain2));

    // when
    List<WorkflowInstanceDomain> all = queryRepository.findAllById("workflow");

    // given
    assertThat(all).hasSize(2);
  }

  @Test
  void findAllByIdAndVersion() {
    // given
    HistoricProcessInstance instance1 = mock(HistoricProcessInstance.class);
    HistoricProcessInstance instance2 = mock(HistoricProcessInstance.class);
    QueryMocks.mockHistoricProcessInstanceQuery(historyService).list(List.of(instance1, instance2));
    ProcessDefinition definition1 = mock(ProcessDefinition.class);
    ProcessDefinition definition2 = mock(ProcessDefinition.class);
    QueryMocks.mockProcessDefinitionQuery(repositoryService).list(List.of(definition1, definition2));
    when(definition1.getId()).thenReturn("definition1");
    when(definition2.getId()).thenReturn("definition2");
    when(definition1.getVersionTag()).thenReturn("1");
    when(definition2.getVersionTag()).thenReturn("1");

    WorkflowInstanceDomain domain1 = WorkflowInstanceDomain.builder()
        .name("workflow")
        .status("active")
        .version(1L)
        .startDate(Instant.now())
        .endDate(Instant.now())
        .instanceId("inst-id1")
        .duration(Duration.ofMillis(5000))
        .id("id1")
        .build();
    WorkflowInstanceDomain domain2 = WorkflowInstanceDomain.builder()
        .name("workflow")
        .status("pending")
        .version(1L)
        .startDate(Instant.now())
        .instanceId("inst-id2")
        .id("id2")
        .build();
    when(objectConverter.convertCollection(anyList(), eq(Map.of("definition2", "1", "definition1", "1")),
        eq(WorkflowInstanceDomain.class))).thenReturn(
        List.of(domain1, domain2));

    // when
    List<WorkflowInstanceDomain> all = queryRepository.findAllByIdAndVersion("workflow", "1");

    // given
    assertThat(all).hasSize(2);
  }

  static Stream<Arguments> instanceStatus() {
    return Stream.of(
        Arguments.arguments(StatusEnum.COMPLETED),
        Arguments.arguments(StatusEnum.FAILED),
        Arguments.arguments(StatusEnum.PENDING)
    );
  }

  @ParameterizedTest()
  @MethodSource("instanceStatus")
  void findAllByIdWithStatus(StatusEnum status) {
    // given
    HistoricProcessInstance instance1 = mock(HistoricProcessInstance.class);
    HistoricProcessInstance instance2 = mock(HistoricProcessInstance.class);
    QueryMocks.mockHistoricProcessInstanceQuery(historyService).list(List.of(instance1, instance2));
    ProcessDefinition definition1 = mock(ProcessDefinition.class);
    ProcessDefinition definition2 = mock(ProcessDefinition.class);
    QueryMocks.mockProcessDefinitionQuery(repositoryService).list(List.of(definition1, definition2));

    WorkflowInstanceDomain domain1 = WorkflowInstanceDomain.builder()
        .name("workflow")
        .status("active")
        .startDate(Instant.now())
        .endDate(Instant.now())
        .instanceId("inst-id1")
        .duration(Duration.ofMillis(5000))
        .id("id1")
        .build();

    when(objectConverter.convertCollection(anyList(), eq(WorkflowInstanceDomain.class))).thenReturn(
        List.of(domain1));

    // when
    List<WorkflowInstanceDomain> all = queryRepository.findAllByIdAndStatus("workflow", status);

    // given
    assertThat(all).hasSize(1);
  }

  @Test
  void findAllByIdWithStatusAndVersion() {
    // given
    HistoricProcessInstance instance1 = mock(HistoricProcessInstance.class);
    HistoricProcessInstance instance2 = mock(HistoricProcessInstance.class);
    QueryMocks.mockHistoricProcessInstanceQuery(historyService).list(List.of(instance1, instance2));
    ProcessDefinition definition1 = mock(ProcessDefinition.class);
    ProcessDefinition definition2 = mock(ProcessDefinition.class);
    QueryMocks.mockProcessDefinitionQuery(repositoryService).list(List.of(definition1, definition2));
    when(definition1.getId()).thenReturn("definition1");
    when(definition2.getId()).thenReturn("definition2");
    when(definition1.getVersionTag()).thenReturn("1");
    when(definition2.getVersionTag()).thenReturn("1");

    WorkflowInstanceDomain domain1 = WorkflowInstanceDomain.builder()
        .name("workflow")
        .status("active")
        .startDate(Instant.now())
        .endDate(Instant.now())
        .instanceId("inst-id1")
        .duration(Duration.ofMillis(5000))
        .id("id1")
        .build();

    when(objectConverter.convertCollection(anyList(), anyMap(), eq(WorkflowInstanceDomain.class))).thenReturn(
        List.of(domain1));

    // when
    List<WorkflowInstanceDomain> all =
        queryRepository.findAllByIdAndStatusAndVersion("workflow", StatusEnum.PENDING, "1");

    // given
    assertThat(all).hasSize(1);
  }
}
