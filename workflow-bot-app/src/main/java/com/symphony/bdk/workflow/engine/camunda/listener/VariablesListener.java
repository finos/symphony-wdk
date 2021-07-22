package com.symphony.bdk.workflow.engine.camunda.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.Expression;

import java.util.Map;

@Slf4j
public class VariablesListener implements ExecutionListener {

  private Expression variables;

  @Override
  public void notify(DelegateExecution execution) throws Exception {
    if (variables != null && variables.getValue(execution) != null) {

      log.info("Setting variables for execution {}", execution.getId());
      Map<String, Object> variablesAsMap = convertJsonStringToMap(variables.getValue(execution).toString());

      if (variablesAsMap != null) {
        log.debug("Loading workflow variable to execution context [{}]", variablesAsMap);
        execution.setVariable("variables", variablesAsMap);
      }
    }
  }

  private Map<String, Object> convertJsonStringToMap(String variableAsString) throws JsonProcessingException {
    return new ObjectMapper().readValue(variableAsString, new TypeReference<>() {});
  }

}
