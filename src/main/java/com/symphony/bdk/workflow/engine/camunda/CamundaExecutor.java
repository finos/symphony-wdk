package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Component
public class CamundaExecutor implements JavaDelegate {

  public static final String IMPL = "impl";
  public static final String ACTIVITY = "activity";

  private final MessageService messageService;
  private final StreamService streamService;

  public CamundaExecutor(MessageService messageService, StreamService streamService) {
    this.messageService = messageService;
    this.streamService = streamService;
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    Class<?> implClass = Class.forName((String) execution.getVariable(IMPL));
    ActivityExecutor<?> executor = (ActivityExecutor<?>) implClass.getDeclaredConstructor().newInstance();

    Type type =
        ((ParameterizedType) (implClass.getGenericInterfaces()[0])).getActualTypeArguments()[0];

    String activityAsJsonString = (String) execution.getVariable(ACTIVITY);
    Object activity = new ObjectMapper().readValue(activityAsJsonString, Class.forName(type.getTypeName()));

    executor.execute(new CamundaActivityExecutorContext(execution, activity));
  }

  private class CamundaActivityExecutorContext<T> implements ActivityExecutorContext<T> {
    private final DelegateExecution execution;
    private final T activity;

    public CamundaActivityExecutorContext(DelegateExecution execution, T activity) {
      this.execution = execution;
      this.activity = activity;
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

    @Override
    public T getActivity() {
      return activity;
    }
  }
}
