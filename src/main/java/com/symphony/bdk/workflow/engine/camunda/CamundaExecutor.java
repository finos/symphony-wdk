package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.SendMessageExecutor;
import com.symphony.bdk.workflow.lang.swadl.activity.SendMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

@Component
public class CamundaExecutor implements JavaDelegate {

  public static final String EXECUTOR = "executor";
  public static final String ACTIVITY = "activity";

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final MessageService messageService;
  private final StreamService streamService;

  public CamundaExecutor(MessageService messageService, StreamService streamService) {
    this.messageService = messageService;
    this.streamService = streamService;
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    Class<?> implClass = Class.forName((String) execution.getVariable(EXECUTOR));
    ActivityExecutor<?> executor = (ActivityExecutor<?>) implClass.getDeclaredConstructor().newInstance();

    Type type =
        ((ParameterizedType) (implClass.getGenericInterfaces()[0])).getActualTypeArguments()[0];

    String activityAsJsonString = (String) execution.getVariable(ACTIVITY);
    Object activity = OBJECT_MAPPER.readValue(activityAsJsonString, Class.forName(type.getTypeName()));

    if (type.getTypeName().equals(SendMessage.class.getTypeName())) {
      this.handleSendMessageActivity((SendMessage) activity,
          (String) execution.getVariable(SendMessageExecutor.INPUT_ROOM_ID_KEY));
    }

    executor.execute(new CamundaActivityExecutorContext(execution, activity));
  }

  private void handleSendMessageActivity(SendMessage sendMessage, String streamId) {
    if (streamId != null) {
      SendMessage.To to = new SendMessage.To();
      to.setStreamId(streamId);
      sendMessage.setTo(to);
    }
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
    public void setVariable(String name, Object value) {
      execution.setVariable(name, value);
    }

    @Override
    public void setOutputVariable(String activityId, String name, Object value) {
      Map<String, Object> innerMap = Collections.singletonMap(name, value);
      Map<String, Object> outerMap = Collections.singletonMap("outputs", innerMap);
      execution.setVariable(activityId, outerMap);
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
