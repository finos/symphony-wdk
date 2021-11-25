package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.workflow.engine.ResourceProvider;
import com.symphony.bdk.workflow.engine.camunda.audit.AuditTrailLogger;
import com.symphony.bdk.workflow.engine.camunda.variable.BpmnToAndFromBaseActivityMixin;
import com.symphony.bdk.workflow.engine.camunda.variable.EscapedJsonVariableDeserializer;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.BdkGateway;
import com.symphony.bdk.workflow.engine.executor.EventHolder;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.slf4j.MDC;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CamundaExecutor implements JavaDelegate {

  public static final String EXECUTOR = "executor";
  public static final String ACTIVITY = "activity";

  public static final ObjectMapper OBJECT_MAPPER;

  // set MDC entries so that executors can produce log that we can contextualize
  private static final String MDC_PROCESS_ID = "PROCESS_ID";
  private static final String MDC_ACTIVITY_ID = "ACTIVITY_ID";

  static {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(List.class, new EscapedJsonVariableDeserializer<>(List.class));
    module.addDeserializer(Map.class, new EscapedJsonVariableDeserializer<>(Map.class));
    OBJECT_MAPPER = JsonMapper.builder()
        .addModule(module)
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        // to escape # or $ in message received content and still serialize it to JSON
        .configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
        .build();
    OBJECT_MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
    // serialized properties must be annotated explicitly with @JsonProperty
    OBJECT_MAPPER.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
    OBJECT_MAPPER.addMixIn(BaseActivity.class, BpmnToAndFromBaseActivityMixin.class);
  }

  private final BdkGateway bdk;
  private final AuditTrailLogger auditTrailLogger;
  private final ResourceProvider resourceLoader;
  private final ApplicationContext applicationContext;

  public CamundaExecutor(BdkGateway bdk, AuditTrailLogger auditTrailLogger,
      @Qualifier("workflowResourcesProvider") ResourceProvider resourceLoader, ApplicationContext applicationContext) {
    this.bdk = bdk;
    this.auditTrailLogger = auditTrailLogger;
    this.resourceLoader = resourceLoader;
    this.applicationContext = applicationContext;
  }

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    Class<?> implClass = Class.forName((String) execution.getVariable(EXECUTOR));

    ActivityExecutor<?> executor;

    // An activity executor can be a bean or not.
    // We firstly try to get it as a bean from Spring application context,
    // if not found, then we catch the exception, and we create a new instance.
    try {
      executor = (ActivityExecutor<?>) applicationContext.getBean(implClass);
    } catch (NoSuchBeanDefinitionException noSuchBeanDefinitionException) {
      executor = (ActivityExecutor<?>) implClass.getDeclaredConstructor().newInstance();
    }


    Type type =
        ((ParameterizedType) (implClass.getGenericInterfaces()[0])).getActualTypeArguments()[0];

    String activityAsJsonString = (String) execution.getVariable(ACTIVITY);
    Object activity = OBJECT_MAPPER.readValue(activityAsJsonString, Class.forName(type.getTypeName()));

    EventHolder event = (EventHolder) execution.getVariable(ActivityExecutorContext.EVENT);

    try {
      setMdc(execution);
      auditTrailLogger.execute(execution, activity.getClass().getSimpleName());
      executor.execute(
          new CamundaActivityExecutorContext(execution, (BaseActivity) activity, event, resourceLoader, bdk));
    } catch (Exception e) {
      log.error(String.format("Activity %s from workflow %s failed",
          execution.getActivityInstanceId(), execution.getProcessInstanceId()), e);
      throw new BpmnError("FAILURE", e);
    } finally {
      clearMdc();
    }
  }

  private void setMdc(DelegateExecution execution) {
    MDC.put(MDC_PROCESS_ID, execution.getProcessInstanceId());
    MDC.put(MDC_ACTIVITY_ID, execution.getActivityInstanceId());
  }

  private void clearMdc() {
    MDC.remove(MDC_PROCESS_ID);
    MDC.remove(MDC_ACTIVITY_ID);
  }

  private static class CamundaActivityExecutorContext<T extends BaseActivity> implements ActivityExecutorContext<T> {
    private final DelegateExecution execution;
    private final T activity;
    private final EventHolder<Object> event;
    private final ResourceProvider resourceLoader;
    private final BdkGateway bdk;

    public CamundaActivityExecutorContext(DelegateExecution execution, T activity, EventHolder<Object> event,
        ResourceProvider resourceLoader, BdkGateway bdk) {
      this.execution = execution;
      this.activity = activity;
      this.event = event;
      this.resourceLoader = resourceLoader;
      this.bdk = bdk;
    }

    @Override
    public void setOutputVariables(Map<String, Object> variables) {
      this.checkNoOutputsExist(activity.getId());

      Map<String, Object> innerMap = new HashMap<>(variables);
      String activityId = getActivity().getId();

      Map<String, Object> outer = Map.of(ActivityExecutorContext.OUTPUTS, innerMap);
      ObjectValue objectValue = Variables.objectValue(outer)
          .serializationDataFormat(Variables.SerializationDataFormats.JSON)
          .create();

      // flatten outputs for message correlation
      Map<String, Object> flattenOutputs = new HashMap<>();

      for (Map.Entry<String, Object> entry : innerMap.entrySet()) {
        // value might not implement serializable or be a collection with non-serializable items, we use JSON if needed
        if (entry.getValue() instanceof Serializable && !(entry.getValue() instanceof Collection)) {
          flattenOutputs.put(entry.getKey(), entry.getValue());
        } else {
          flattenOutputs.put(entry.getKey(), Variables.objectValue(entry.getValue())
              .serializationDataFormat(Variables.SerializationDataFormats.JSON)
              .create());
        }
      }

      execution.setVariable(activityId, objectValue);
      flattenOutputs.forEach((key, value) -> execution.setVariable(
          String.format("%s.%s.%s", activityId, ActivityExecutorContext.OUTPUTS, key), value));
    }

    @Override
    public void setOutputVariable(String name, Object value) {
      this.checkNoOutputsExist(activity.getId());
      Map<String, Object> singletonMap = new HashMap<>();
      singletonMap.put(name, value);
      this.setOutputVariables(singletonMap);
    }

    @Override
    public BdkGateway bdk() {
      return bdk;
    }

    @Override
    public T getActivity() {
      return activity;
    }

    @Override
    public EventHolder<Object> getEvent() {
      return event;
    }

    @Override
    public String getProcessInstanceId() {
      return this.execution.getProcessInstanceId();
    }

    @Override
    public String getCurrentActivityId() {
      return this.execution.getCurrentActivityId();
    }

    @Override
    public InputStream getResource(Path resourcePath) throws IOException {
      return resourceLoader.getResource(resourcePath);
    }

    @Override
    public Path saveResource(Path resourcePath, byte[] content) throws IOException {
      return resourceLoader.saveResource(resourcePath, content);
    }

    private void checkNoOutputsExist(String activityId) {
      List<String> foundOutputs = this.execution.getVariables()
          .keySet()
          .stream()
          .filter(o -> o.contains(String.format("%s.outputs", activityId)))
          .collect(Collectors.toList());

      if (!foundOutputs.isEmpty()) {
        throw new RuntimeException(
            String.format("Outputs %s already exist for activity %s", foundOutputs, activityId));
      }
    }
  }
}
