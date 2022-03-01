package com.symphony.bdk.workflow.engine.camunda.variable;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

import java.util.Map;

/**
 * Merges form variables with activity variables.
 * <p>When a form reply is received, the reply data is sent to a specific map named "form" and passed as a variable to
 * the process. Here we take it and merge it with the existing variable ACTIVITY_ID
 * (that contains the outputs for instance).
 * </p>
 * <p>We cannot set it directly when the message is sent because it overwrites the existing variable ACTIVITY_ID.</p>
 * <p>This is executed when a correlation message for a form is received to merge the variables as soon as possible.</p>
 */
@Slf4j
public class FormVariableListener implements ExecutionListener {

  public static final String FORM_VARIABLES = "form";

  @SuppressWarnings("unchecked")
  @Override
  public void notify(DelegateExecution execution) {
    Object formVariable = execution.getVariable(FORM_VARIABLES);
    // do we have a form variable in the current process?
    if (formVariable instanceof Map) {
      Map<String, Map<String, Object>> form = (Map<String, Map<String, Object>>) formVariable;
      // there should be only one form.FORM_ID entry
      for (Map.Entry<String, Map<String, Object>> entry : form.entrySet()) {
        Object activityVariable = execution.getVariable(entry.getKey());
        if (activityVariable instanceof Map) {
          // merge form.FORM_ID.FORM_REPLY_DATA into ACTIVITY_ID...
          // FORM_ID = ACTIVITY_ID
          // in the end we have ACTIVITY_ID.outputs... and ACTIVITY_ID.FORM_REPLY_DATA in the same variable
          Map<String, Object> activity = (Map<String, Object>) activityVariable;
          activity.putAll(entry.getValue());
        }
      }
      execution.removeVariable(FORM_VARIABLES);
    }
  }

}
