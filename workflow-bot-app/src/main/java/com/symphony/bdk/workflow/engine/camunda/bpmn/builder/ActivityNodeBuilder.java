package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.CamundaExecutor;
import com.symphony.bdk.workflow.engine.camunda.audit.ScriptTaskAuditListener;
import com.symphony.bdk.workflow.engine.camunda.bpmn.BuildProcessContext;
import com.symphony.bdk.workflow.swadl.ActivityRegistry;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
import com.symphony.bdk.workflow.swadl.v1.activity.ExecuteScript;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.springframework.stereotype.Component;

@Component
public class ActivityNodeBuilder extends AbstractNodeBpmnBuilder {

  @Override
  public AbstractFlowNodeBuilder<?, ?> build(WorkflowNode element, String parentId,
      AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context) throws JsonProcessingException {
    return addTask(builder, element.getActivity(), context);
  }

  @Override
  public WorkflowNodeType type() {
    return WorkflowNodeType.ACTIVITY;
  }

  protected AbstractFlowNodeBuilder<?, ?> addTask(AbstractFlowNodeBuilder<?, ?> eventBuilder, BaseActivity activity,
      BuildProcessContext context) throws JsonProcessingException {
    // hardcoded so we can rely on Camunda's script task instead of a service task
    if (activity instanceof ExecuteScript) {
      return addScriptTask(eventBuilder, (ExecuteScript) activity);
    }
    return addServiceTask(eventBuilder, activity);
  }

  private AbstractFlowNodeBuilder<?, ?> addScriptTask(AbstractFlowNodeBuilder<?, ?> builder,
      ExecuteScript scriptActivity) {
    return builder.scriptTask()
        .id(scriptActivity.getId())
        .name(scriptActivity.getId())
        .scriptText(scriptActivity.getScript())
        .scriptFormat(ExecuteScript.SCRIPT_ENGINE)
        .camundaExecutionListenerClass(ExecutionListener.EVENTNAME_START, ScriptTaskAuditListener.class);
  }

  private AbstractFlowNodeBuilder<?, ?> addServiceTask(AbstractFlowNodeBuilder<?, ?> builder, BaseActivity activity)
      throws JsonProcessingException {
    return builder.serviceTask()
        .id(activity.getId())
        .name(activity.getId())
        .camundaClass(CamundaExecutor.class)
        .camundaInputParameter(CamundaExecutor.EXECUTOR,
            ActivityRegistry.getActivityExecutors().get(activity.getClass()).getName())
        .camundaInputParameter(CamundaExecutor.ACTIVITY,
            CamundaExecutor.OBJECT_MAPPER.writeValueAsString(activity));
  }
}
