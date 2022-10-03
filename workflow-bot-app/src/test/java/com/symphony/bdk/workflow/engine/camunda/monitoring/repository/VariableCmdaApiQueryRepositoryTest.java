package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import org.assertj.core.util.Maps;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricDetail;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.camunda.community.mockito.QueryMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class VariableCmdaApiQueryRepositoryTest {
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
    VariablesDomain global = queryRepository.findVarsByWorkflowInstanceIdAndVarName("id", "variables");
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
    List<VariablesDomain> global = queryRepository.findGlobalVarsHistoryByWorkflowInstId("id", null, null);
    // then
    assertThat(global).hasSize(1);
    assertThat(global.get(0).getRevision()).isEqualTo(1);
    assertThat(global.get(0).getOutputs()).hasSize(1);
    assertThat(global.get(0).getUpdateTime()).isNotNull();
  }
}
