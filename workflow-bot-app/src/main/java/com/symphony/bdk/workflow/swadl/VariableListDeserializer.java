package com.symphony.bdk.workflow.swadl;

import com.symphony.bdk.workflow.swadl.v1.Variable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * To support SWADL to model deserialization using either variable references (${}) or
 * values (lists, maps, strings, numbers).
 */
@SuppressWarnings("rawtypes")
public class VariableListDeserializer extends StdDeserializer<Variable> {

  public VariableListDeserializer() {
    super(Variable.class);
  }

  @Override
  public Variable deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ObjectMapper mapper = (ObjectMapper) p.getCodec();
    JsonNode node = mapper.readTree(p);

    if (node.isTextual()) {
      // this is a variable reference, keep it as such
      return new Variable<>(node.asText());

      // otherwise, store the value directly
    } else if (node.isBoolean()) {
      return Variable.value(node.booleanValue());

    } else if (node.isNumber()) {
      return Variable.value(node.numberValue());

    } else if (node.isArray()) {
      return Variable.value(mapper.convertValue(node, new TypeReference<List<Variable>>() {}));

    } else if (node.isObject()) {
      return Variable.value(mapper.treeToValue(node, Map.class));

    } else {
      throw JsonMappingException.from(p,
          "Unsupported type for attribute (not a variable reference or a list/map/boolean/number/string)");
    }
  }

}
