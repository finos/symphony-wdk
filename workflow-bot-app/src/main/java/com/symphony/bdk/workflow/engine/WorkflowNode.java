package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.workflow.swadl.v1.Event;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class WorkflowNode {
  private String id;
  private Event event;
  private BaseActivity activity;
  private WorkflowNodeType elementType;
  private Map<String, String> ifConditions = new HashMap<>();

  public WorkflowNode id(String id) {
    this.id = id;
    return this;
  }

  public WorkflowNode activity(BaseActivity activity) {
    this.activity = activity;
    return this;
  }

  public WorkflowNode event(Event event) {
    this.event = event;
    return this;
  }

  public WorkflowNode elementType(WorkflowNodeType elementType) {
    this.elementType = elementType;
    return this;
  }

  public WorkflowNode addIfCondition(String parentId, String ifCondition) {
    this.ifConditions.put(parentId, ifCondition);
    return this;
  }

  public boolean isConditional() {
    return !this.ifConditions.isEmpty();
  }

  public String getIfCondition(String parentId) {
    return this.ifConditions.get(parentId);
  }
}
