package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.bpmn.BuildProcessContext;
import com.symphony.bdk.workflow.engine.camunda.variable.FormVariableListener;
import com.symphony.bdk.workflow.swadl.v1.EventWithTimeout;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.EventSubProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.ParallelGatewayBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FormRepliedNodeBuilder extends AbstractNodeBpmnBuilder {
  /**
   * A form reply is a dedicated sub process doing 2 things:
   * - waiting for an expiration time, if the expiration time is reached the entire subprocess ends
   * and replies are no longer used
   * - waiting for reply with an event sub process that is running for each reply
   */
  @Override
  public AbstractFlowNodeBuilder<?, ?> build(WorkflowNode element, String parentId,
      AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context) {
    if (builder instanceof ParallelGatewayBuilder) {
      return builder.intermediateCatchEvent()
          .camundaAsyncBefore()
          .name(element.getId())
          .message(element.getId());
    } else {
      // cache the sub process builder, the form reply might have a brother event,
      // which is going to use this cached builder
      SubProcessBuilder subProcess = builder.subProcess();
      context.cacheSubProcess(subProcess);

      timeoutFlow(element, subProcess, context);
      // we add the form reply event sub process inside the subprocess
      EventSubProcessBuilder subProcessBuilder = subProcess.camundaAsyncBefore().embeddedSubProcess().eventSubProcess();
      // cache the sub process builder, so to terminate it later
      context.cacheEventSubProcessToDone(subProcessBuilder);

      return subProcessBuilder.startEvent()
          .camundaExecutionListenerClass(ExecutionListener.EVENTNAME_START, FormVariableListener.class)
          .camundaAsyncBefore()
          // run multiple instances of the sub process (i.e. multiple replies) if it's true,
          // otherwise execute only once, as exclusive
          .interrupting(element.getEvent().getFormReplied().getExclusive())
          .message(element.getId())
          .name(element.getId());
    }
  }

  private void timeoutFlow(WorkflowNode element, SubProcessBuilder subProcess, BuildProcessContext context) {
    String timeout = readTimeout(element);
    subProcess.embeddedSubProcess()
        .startEvent()
        .intermediateCatchEvent().timerWithDuration(timeout).serviceTask().camundaExpression("${true}")
        .camundaExecutionListenerClass(ExecutionListener.EVENTNAME_START, ThrowTimeoutDelegate.class.getName())
        .endEvent();
  }

  private String readTimeout(WorkflowNode element) {
    String timeout = DEFAULT_FORM_REPLIED_EVENT_TIMEOUT;
    if (element.getEvent() instanceof EventWithTimeout) {
      timeout = ((EventWithTimeout) element.getEvent()).getTimeout();
    }
    return timeout;
  }

  @Override
  public WorkflowNodeType type() {
    return WorkflowNodeType.FORM_REPLIED_EVENT;
  }

  public static class ThrowTimeoutDelegate implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) throws Exception {
      log.debug("Form reply event is timeout.");
      throw new BpmnError(ERROR_CODE, "Form reply event is timeout.");
    }
  }
}
