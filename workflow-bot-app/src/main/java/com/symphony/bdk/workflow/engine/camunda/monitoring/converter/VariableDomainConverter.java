package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import org.camunda.bpm.engine.history.HistoricDetail;
import org.camunda.bpm.engine.history.HistoricVariableUpdate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VariableDomainConverter implements Converter<HistoricDetail, VariablesDomain> {

  @Override
  @SuppressWarnings("unchecked")
  public VariablesDomain apply(HistoricDetail historicVariableUpdate) {
    HistoricVariableUpdate variableUpdate = (HistoricVariableUpdate) historicVariableUpdate;
    VariablesDomain domain = new VariablesDomain();
    domain.setOutputs((Map<String, Object>) variableUpdate.getValue());
    domain.setRevision(variableUpdate.getRevision());
    domain.setUpdateTime(variableUpdate.getTime().toInstant());
    return domain;
  }
}
