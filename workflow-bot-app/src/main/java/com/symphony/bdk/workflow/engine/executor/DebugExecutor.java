package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.workflow.swadl.v1.activity.Debug;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class DebugExecutor implements ActivityExecutor<Debug> {
  private static final String OUTPUT_OBJECT = "object";
  private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  @Override
  public void execute(ActivityExecutorContext<Debug> execution) throws IOException {
    Object object = execution.getActivity().getObject();
    try {
      JsonNode jsonNode = objectMapper.readTree(object.toString());
      String s = objectMapper.writeValueAsString(jsonNode);
      log.debug(s);
      execution.setOutputVariables(Map.of(OUTPUT_OBJECT, s));

    } catch (JsonProcessingException jsonProcessingException) {
      log.debug(object.toString());
      execution.setOutputVariables(Map.of(OUTPUT_OBJECT, object.toString()));
    }
  }

}
