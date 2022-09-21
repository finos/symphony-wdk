package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VariableCmdaApiQueryRepositoryTest {/*
  @Mock HistoryService historyService;
  @Mock ObjectConverter objectConverter;
  @InjectMocks VariableCmdaApiQueryRepository queryRepository;

  @Test
  void findGlobalByWorkflowInstanceId() {
    // given
    HistoricVariableInstanceEntity variables = mock(HistoricVariableInstanceEntity.class);
    QueryMocks.mockHistoricVariableInstanceQuery(historyService).singleResult(variables);
    when(variables.getValue()).thenReturn(Maps.newHashMap("key", "value"));
    when(variables.getRevision()).thenReturn(1);
    when(variables.getCreateTime()).thenReturn(new Date());
    // when
    VariablesDomain global = queryRepository.findGlobalVarsByWorkflowInstanceId("id");
    // then
    assertThat(global.getOutputs()).hasSize(1);
    assertThat(global.getOutputs().get("key")).isEqualTo("value");
  }

  @Test
  void testFindGlobalVarsHistoryByWorkflowInstId() {
    // given
    HistoricVariableInstanceEntity variables = mock(HistoricVariableInstanceEntity.class);
    QueryMocks.mockHistoricVariableInstanceQuery(historyService).singleResult(variables);

    HistoricDetail details = mock(HistoricDetail.class);
    QueryMocks.mockHistoricDetailQuery(historyService).list(List.of(details));

    VariablesDomain domain = new VariablesDomain();
    domain.setUpdateTime(Instant.now());
    domain.setRevision(1);
    domain.setOutputs(Maps.newHashMap("key", "value"));
    when(objectConverter.convertCollection(anyList(), eq(HistoricDetail.class), eq(VariablesDomain.class))).thenReturn(
        List.of(domain));

    // when
    List<VariablesDomain> global = queryRepository.findGlobalVarsHistoryByWorkflowInstId("id");
    // then
    assertThat(global).hasSize(1);
    assertThat(global.get(0).getRevision()).isEqualTo(1);
    assertThat(global.get(0).getOutputs()).hasSize(1);
    assertThat(global.get(0).getUpdateTime()).isNotNull();
  }*/
}
