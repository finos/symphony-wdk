package com.symphony.bdk.workflow.engine.camunda.variable;

import static com.symphony.bdk.workflow.swadl.v1.Variable.RESOLVED_VALUE_FIELD;
import static com.symphony.bdk.workflow.swadl.v1.Variable.VARIABLE_REFERENCE_FIELD;

import com.symphony.bdk.workflow.swadl.v1.Variable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Variables are serialized as escaped JSON by {@link VariableToJsonConverter} and passed to the executor.
 * When the executor deserializes the activity to access the model, it uses this custom deserializer to
 * recreate the {@link Variable} objects now containing resolved values.
 */
@SuppressWarnings("rawtypes")
public class EscapedJsonVariableDeserializer extends StdDeserializer<Variable> {

  public EscapedJsonVariableDeserializer() {
    super(Variable.class);
  }

  @Override
  public Variable deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ObjectMapper mapper = (ObjectMapper) p.getCodec();
    JsonNode node = mapper.readTree(p);

    if (node.get(VARIABLE_REFERENCE_FIELD).isTextual()) {
      // the variable has been resolved and is escaped JSON, parse it
      try {
        JsonNode tree = mapper.readTree(node.get(VARIABLE_REFERENCE_FIELD).textValue());

        if (tree.isNumber()) {
          return Variable.value(tree.numberValue());

        } else if (tree.isBoolean()) {
          return Variable.value(tree.booleanValue());

        } else if (tree.isArray()) {
          List variable;
          try {
            variable =
                mapper.convertValue(node.get(VARIABLE_REFERENCE_FIELD).textValue(),
                    new TypeReference<List<Variable>>() {});
          } catch (IllegalArgumentException e) {
            // this is just a list of strings
            variable = mapper.readValue(node.get(VARIABLE_REFERENCE_FIELD).textValue(), List.class);
            // rewrap it in a variable
            variable = (List) variable.stream()
                .map(o -> Variable.value(o))
                .collect(Collectors.toList());
          }
          return Variable.value(variable);


        } else {
          Map variable = mapper.readValue(node.get(VARIABLE_REFERENCE_FIELD).textValue(), Map.class);
          return Variable.value(variable);
        }
      } catch (JsonProcessingException e) {
        // cannot parse the escaped JSON as JSON, treat it as a simple string
        return Variable.value(node.get(VARIABLE_REFERENCE_FIELD).textValue());
      }

      // the variable was already containing its value (no variable reference was used), read it directly
    } else if (node.get(RESOLVED_VALUE_FIELD).isBoolean()) {
      return Variable.value(node.get(RESOLVED_VALUE_FIELD).booleanValue());

    } else if (node.get(RESOLVED_VALUE_FIELD).isNumber()) {
      return Variable.value(node.get(RESOLVED_VALUE_FIELD).numberValue());

    } else if (node.get(RESOLVED_VALUE_FIELD).isTextual()) {
      return Variable.value(node.get(RESOLVED_VALUE_FIELD).textValue());

    } else if (node.get(RESOLVED_VALUE_FIELD).isArray()) {
      List variable = mapper.convertValue(node.get(RESOLVED_VALUE_FIELD), new TypeReference<List<Variable>>() {});
      return Variable.value(variable);

    } else {
      Map variable = mapper.treeToValue(node.get(RESOLVED_VALUE_FIELD), Map.class);
      return Variable.value(variable);
    }
  }

}
