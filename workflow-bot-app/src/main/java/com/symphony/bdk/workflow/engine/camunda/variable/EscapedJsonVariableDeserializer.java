package com.symphony.bdk.workflow.engine.camunda.variable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;


/**
 * Handles maps or lists being stored as JSON escaped strings, as a result of {@link VariableToJsonConverter} doing the
 * variable replacement in the activity's JSON representation.
 */
public class EscapedJsonVariableDeserializer<T> extends JsonDeserializer<T>
    implements ContextualDeserializer {

  // use an internal object mapper to avoid recursion when we read the escaped JSON
  private static final ObjectMapper MAPPER;

  static {
    MAPPER = JsonMapper.builder().build();
    MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
  }

  private final Class<T> containerType;
  private JavaType containedType;

  public EscapedJsonVariableDeserializer(Class<T> containerType) {
    this.containerType = containerType;
  }

  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
    if (property == null) {
      return this;
    }
    EscapedJsonVariableDeserializer<T> deserializer = new EscapedJsonVariableDeserializer<>(this.containerType);
    deserializer.containedType =
        MAPPER.getTypeFactory()
            .constructParametricType(containerType,
                property.getType().getBindings().getTypeParameters().toArray(new JavaType[] {}));
    return deserializer;
  }

  @Override
  public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ObjectMapper mapper = (ObjectMapper) p.getCodec();
    JsonNode node = mapper.readTree(p);

    if (node.isTextual()) {
      // we are expecting a collection but got a string, this is probably escaped JSON
      // (i.e. a variable has been replaced)
      // so we read the unescaped content as the container, recalling this same custom deserializer
      return mapper.readValue(node.asText(), containerType);
    } else {
      // this is a collection, read it as such
      if (containedType == null) {
        return MAPPER.convertValue(node, containerType);
      } else {
        return MAPPER.convertValue(node, containedType);
      }
    }
  }

}
