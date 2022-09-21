package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowInstCmdaApiQueryRepositoryTest {/*
  @Mock HistoryService historyService;
  @Mock ObjectConverter objectConverter;
  @InjectMocks WorkflowInstCmdaApiQueryRepository queryRepository;

  @Test
  void findAllById() {
    // given
    HistoricProcessInstance instance1 = mock(HistoricProcessInstance.class);
    HistoricProcessInstance instance2 = mock(HistoricProcessInstance.class);
    QueryMocks.mockHistoricProcessInstanceQuery(historyService).list(List.of(instance1, instance2));

    WorkflowInstanceDomain domain1 = WorkflowInstanceDomain.builder()
        .name("workflow")
        .status("active")
        .startDate(Instant.now())
        .endDate(Instant.now())
        .instanceId("inst-id1")
        .duration(Duration.ofMillis(5000))
        .id("id1")
        .build();
    WorkflowInstanceDomain domain2 = WorkflowInstanceDomain.builder()
        .name("workflow")
        .status("active")
        .startDate(Instant.now())
        .endDate(Instant.now())
        .instanceId("inst-id2")
        .duration(Duration.ofMillis(5000))
        .id("id2")
        .build();
    when(objectConverter.convertCollection(anyList(), eq(WorkflowInstanceDomain.class))).thenReturn(
        List.of(domain1, domain2));

    // when
    List<WorkflowInstanceDomain> all = queryRepository.findAllById("workflow");

    // given
    assertThat(all).hasSize(2);
  }*/
}
