package com.symphony.bdk.workflow.engine.camunda.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class VariablesListener implements ExecutionListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(VariablesListener.class);

  private Expression variables;

  @Override
  public void notify(DelegateExecution execution) throws Exception {
    if (variables != null && variables.getValue(execution) != null) {

      LOGGER.info("Setting variables for execution {}", execution.getId());

      Map<String, Object> variablesAsMap = convertJsonStringToMap(variables.getValue(execution).toString());

      if (variablesAsMap != null && !variablesAsMap.isEmpty()) {

        LOGGER.debug("Loading workflow variable to execution context [{}]", variablesAsMap);
        execution.setVariable("variables", variablesAsMap);

      }
    }
  }

  private Map<String, Object> convertJsonStringToMap(String variableAsString) throws JsonProcessingException {
    return new ObjectMapper().readValue(variableAsString, new TypeReference<Map<String, Object>>(){});
  }






}
