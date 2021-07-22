package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;
import com.symphony.bdk.workflow.lang.swadl.activity.BaseActivity;

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
  public static final String EVENT = "event";

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

    String eventAsJsonString = (String) execution.getVariable(EVENT);
    EventHolder event = OBJECT_MAPPER.readValue(eventAsJsonString, EventHolder.class);

    executor.execute(new CamundaActivityExecutorContext(execution, (BaseActivity) activity, event));
  }

  private class CamundaActivityExecutorContext<T extends BaseActivity> implements ActivityExecutorContext<T> {
    private final DelegateExecution execution;
    private final T activity;
    private final EventHolder<Object> event;

    public CamundaActivityExecutorContext(DelegateExecution execution, T activity, EventHolder<Object> event) {
      this.execution = execution;
      this.activity = activity;
      this.event = event;
    }

    @Override
    public void setOutputVariable(String name, Object value) {
      Map<String, Object> innerMap = Collections.singletonMap(name, value);
      Map<String, Object> outerMap = Collections.singletonMap("outputs", innerMap);
      String activityId = getActivity().getId();
      execution.setVariable(activityId, outerMap);
      // flatten it too for message correlation
      execution.setVariable(activityId + ".outputs." + name, value); // TODO move "outputs" to constants
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

    @Override
    public EventHolder<Object> getEvent() {
      return event;
    }
  }
}
