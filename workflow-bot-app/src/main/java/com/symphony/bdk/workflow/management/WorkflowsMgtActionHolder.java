package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowsMgtActionHolder {
  private final Map<WorkflowMgtAction, WorkflowsMgtAction> registry = new EnumMap<>(WorkflowMgtAction.class);

  public WorkflowsMgtActionHolder(List<WorkflowsMgtAction> actions) {
    actions.forEach(action -> registry.put(action.action(), action));
  }

  public WorkflowsMgtAction getInstance(WorkflowMgtAction action) {
    return registry.get(action);
  }
}
