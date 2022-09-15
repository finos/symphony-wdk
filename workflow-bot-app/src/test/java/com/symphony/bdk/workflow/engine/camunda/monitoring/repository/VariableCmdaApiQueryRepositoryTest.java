package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import org.assertj.core.util.Maps;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.camunda.community.mockito.QueryMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

@ExtendWith(MockitoExtension.class)
class VariableCmdaApiQueryRepositoryTest {
  @Mock HistoryService historyService;
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
    VariablesDomain global = queryRepository.findGlobalByWorkflowInstanceId("id");
    // then
    then(global.getOutputs()).hasSize(1);
    then(global.getOutputs().get("key")).isEqualTo("value");
  }
}
