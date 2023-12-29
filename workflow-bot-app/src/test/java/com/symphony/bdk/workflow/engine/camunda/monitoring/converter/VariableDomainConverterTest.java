package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import org.assertj.core.util.Maps;
import org.camunda.bpm.engine.history.HistoricVariableUpdate;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VariableDomainConverterTest {

  @Test
  void apply() {
    HistoricVariableUpdate detail = mock(HistoricVariableUpdate.class);
    when(detail.getRevision()).thenReturn(1);
    when(detail.getTime()).thenReturn(new Date());
    when(detail.getValue()).thenReturn(Maps.newHashMap("key", "value"));

    VariableDomainConverter converter = new VariableDomainConverter();
    VariablesDomain domain = converter.apply(detail);

    assertThat(domain.getRevision()).isEqualTo(1);
    assertThat(domain.getOutputs()).hasSize(1);
    assertThat(domain.getUpdateTime()).isNotNull();
  }
}
