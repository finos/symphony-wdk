package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class CamundaExecutor implements JavaDelegate {

  public static final String IMPL = "impl";

  private final MessageService messageService;
  private final StreamService streamService;

  public CamundaExecutor(MessageService messageService, StreamService streamService) {
    this.messageService = messageService;
    this.streamService = streamService;
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    String impl = (String) execution.getVariable(IMPL);
    ActivityExecutor executor = (ActivityExecutor) Class.forName(impl).getDeclaredConstructor().newInstance();
    executor.execute(new CamundaActivityExecutorContext(execution));
  }

  private class CamundaActivityExecutorContext implements ActivityExecutorContext {
    private final DelegateExecution execution;

    public CamundaActivityExecutorContext(DelegateExecution execution) {
      this.execution = execution;
    }

    @Override
    public String getVariable(String name) {
      return (String) execution.getVariable(name);
    }

    @Override
    public void setVariable(String name, String value) {
      execution.setVariable(name, value);
    }

    @Override
    public MessageService messages() {
      return messageService;
    }

    @Override
    public StreamService streams() {
      return streamService;
    }
  }
}
