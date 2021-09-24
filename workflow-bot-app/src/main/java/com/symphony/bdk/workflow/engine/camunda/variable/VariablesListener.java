package com.symphony.bdk.workflow.engine.camunda.variable;

import com.symphony.bdk.workflow.engine.camunda.CamundaExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaField;

import java.util.Map;

/**
 * A process listener to create the variables in the execution context.
 */
@Slf4j
public class VariablesListener implements ExecutionListener {

  private static final String VARIABLES_FIELD = "variables";

  // automatically injected by Camunda
  private Expression variables;

  public static CamundaExecutionListener create(BpmnModelInstance instance, Map<String, Object> variables)
      throws JsonProcessingException {
    CamundaExecutionListener listener = instance.newInstance(CamundaExecutionListener.class);
    listener.setCamundaEvent(EVENTNAME_START);
    listener.setCamundaClass(VariablesListener.class.getName());

    CamundaField field = instance.newInstance(CamundaField.class);
    field.setCamundaName(VARIABLES_FIELD);
    field.setCamundaStringValue(CamundaExecutor.OBJECT_MAPPER.writeValueAsString(variables));
    listener.getCamundaFields().add(field);

    return listener;
  }

  @Override
  public void notify(DelegateExecution execution) throws Exception {
    if (variables != null && variables.getValue(execution) != null) {

      log.debug("Setting variables for execution {}", execution.getId());
      Map<String, Object> variablesAsMap = convertJsonStringToMap(variables.getValue(execution).toString());
      if (variablesAsMap != null) {
        log.debug("Loading workflow variable to execution context [{}]", variablesAsMap.keySet());
        execution.setVariable(ActivityExecutorContext.VARIABLES, variablesAsMap);
      }
    }
  }

  private Map<String, Object> convertJsonStringToMap(String variableAsString) throws JsonProcessingException {
    return CamundaExecutor.OBJECT_MAPPER.readValue(variableAsString, new TypeReference<>() {});
  }

}
