package com.symphony.bdk.workflow.engine.camunda.variable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.impl.javax.el.ELException;
import org.camunda.bpm.engine.impl.juel.TypeConverterImpl;

/**
 * When variables are resolved in expressions, serialize the complex objects (lists, maps) as escaped JSON
 * so they can be rebuilt to complex object when the model is deserialized and used in the executors.
 *
 * <p>This is used by the src/main/resources/el.properties file, loaded by
 * {@link org.camunda.bpm.engine.impl.juel.ExpressionFactoryImpl}.
 */
public class VariableToJsonConverter extends TypeConverterImpl {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  protected String coerceToString(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof String) {
      return (String) value;
    }
    if (value instanceof Enum<?>) {
      return ((Enum<?>) value).name();
    }
    try {
      // the entire activity is serialized as JSON already, so we serialize the variable resolved value as escaped JSON.
      String json = OBJECT_MAPPER.writeValueAsString(value);
      char[] escapedJson = JsonStringEncoder.getInstance().quoteAsString(json);
      return new String(escapedJson);
    } catch (JsonProcessingException e) {
      throw new ELException(e);
    }
  }
}
