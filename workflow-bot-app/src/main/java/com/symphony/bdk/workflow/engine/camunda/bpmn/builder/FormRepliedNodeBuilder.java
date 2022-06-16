package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.bpmn.BuildProcessContext;
import com.symphony.bdk.workflow.engine.camunda.variable.FormVariableListener;
import com.symphony.bdk.workflow.swadl.v1.EventWithTimeout;
import com.symphony.bdk.workflow.swadl.v1.event.FormRepliedEvent;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.EventSubProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;
import org.springframework.stereotype.Component;

@Component
public class FormRepliedNodeBuilder extends AbstractNodeBpmnBuilder {
  private static final String PT_24_H = "PT24H";

  @Override
  public AbstractFlowNodeBuilder<?, ?> build(WorkflowNode element, AbstractFlowNodeBuilder<?, ?> builder,
      BuildProcessContext context) {
    FormRepliedEvent formReplied = element.getEvent().getFormReplied();
    if (formReplied.getExclusive()) {
      builder = builder.intermediateCatchEvent().camundaAsyncBefore().name(element.getId()).message(element.getId())
          .camundaExecutionListenerClass(ExecutionListener.EVENTNAME_START, FormVariableListener.class);
    } else {
      /*
          A form reply is a dedicated sub process doing 2 things:
            - waiting for an expiration time, if the expiration time is reached the entire subprocess ends
              and replies are no longer used
            - waiting for reply with an event sub process that is running for each reply
       */
      SubProcessBuilder subProcess = builder.subProcess();
      timeoutFlow(element, subProcess);
      // we add the form reply event sub process inside the subprocess
      EventSubProcessBuilder subProcessBuilder = subProcess.camundaAsyncBefore().embeddedSubProcess()
          .eventSubProcess();
      // cache the sub process builder, so to terminate it later
      context.addSubProcessToBeDone(subProcessBuilder);

      builder = subProcessBuilder.startEvent()
          .camundaExecutionListenerClass(ExecutionListener.EVENTNAME_START, FormVariableListener.class)
          .camundaAsyncBefore()
          .interrupting(false) // run multiple instances of the sub process (i.e. multiple replies)
          .message(element.getId())
          .name(element.getId());
    }
    return builder;
  }

  private void timeoutFlow(WorkflowNode element, SubProcessBuilder subProcess) {
    String timeout = PT_24_H;
    if (element.getEvent() instanceof EventWithTimeout) {
      timeout = ((EventWithTimeout) element.getEvent()).getTimeout();
    }
    subProcess.embeddedSubProcess()
        .startEvent()
        .intermediateCatchEvent().timerWithDuration(timeout);
  }

  @Override
  public WorkflowNodeType type() {
    return WorkflowNodeType.FORM_REPLIED_EVENT;
  }
}
