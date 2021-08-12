package com.symphony.bdk.workflow.swadl.validator;

import com.symphony.bdk.workflow.swadl.ActivityRegistry;
import com.symphony.bdk.workflow.swadl.exception.SwadlNotValidException;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListReportProvider;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This class validates a SWADL workflow written in YAML.
 */
@Slf4j
public class YamlValidator {

  public static final String YAML_VALIDATION_COMMAND = "/validate";

  private static final String JSON_SCHEMA_FILE = "/json-schema.json";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final ObjectMapper YAML_READER = new ObjectMapper(new YAMLFactory());
  private static final JsonSchemaFactory JSON_SCHEMA_FACTORY = JsonSchemaFactory.newBuilder()
      // filter out warnings such as custom x-documentation properties
      .setReportProvider(new ListReportProvider(LogLevel.ERROR, LogLevel.FATAL))
      .freeze();

  private YamlValidator() {
  }

  public static void validateYaml(String yaml) throws IOException, ProcessingException {
    try (InputStream schemaStream = YamlValidator.class.getResourceAsStream(JSON_SCHEMA_FILE)) {
      if (schemaStream == null) {
        throw new IOException("Could not read JSON schema from classpath location: " + JSON_SCHEMA_FILE);
      }
      validate(yaml, OBJECT_MAPPER.readTree(schemaStream));
    }
  }

  private static void validate(String yaml, JsonNode jsonSchema)
      throws ProcessingException, IOException {

    addCustomActivitiesToSchema(jsonSchema);

    final JsonSchema schema = JSON_SCHEMA_FACTORY.getJsonSchema(jsonSchema);
    try {
      JsonNode yamlTree = YAML_READER.readTree(yaml);
      ProcessingReport report = schema.validate(yamlTree);

      if (!report.isSuccess()) {
        YamlJsonPointer yamlJsonPointer = new YamlJsonPointer(new StringReader(yaml));
        List<SwadlError> errors =
            StreamSupport.stream(Spliterators.spliteratorUnknownSize(report.iterator(), Spliterator.ORDERED), false)
                .map(e -> ProcessingMessageToSwadlError.convert(yamlTree, yamlJsonPointer, e))
                .collect(Collectors.toList());
        throw new SwadlNotValidException(errors, report.toString());
      }
    } catch (JsonProcessingException e) {
      throw new SwadlNotValidException(e);
    }
  }

  private static void addCustomActivitiesToSchema(JsonNode jsonSchema) {
    // we expect activities to be defined this way in the JSON schema
    ArrayNode activityItems = (ArrayNode) jsonSchema.get("properties").get("activities").get("items").get("oneOf");

    // clear last item which is there to support completion/validation with custom activities
    activityItems.remove(activityItems.size() - 1);

    // builtin activities are already in JSON Schema
    Set<String> alreadyDefinedActivities = StreamSupport.stream(activityItems.spliterator(), false)
        .map(node -> node.get("$ref").asText())
        .map(ref -> ref.replace("#/definitions/", ""))
        .collect(Collectors.toSet());

    for (Class<? extends BaseActivity> activityType : ActivityRegistry.getActivityTypes()) {
      // in YAML, activities are referenced like my-activity and the class is named MyActivity
      String activityTypeName = camelToKebabCase(activityType.getSimpleName());
      if (!alreadyDefinedActivities.contains(activityTypeName)) {
        addJsonSchemaForCustomActivity(activityItems, activityTypeName);
        // avoid defining an activity twice, however deserialization checks for duplicates and will fail
        alreadyDefinedActivities.add(activityTypeName);
      }
    }
  }

  private static void addJsonSchemaForCustomActivity(ArrayNode activityItems, String activityTypeName) {
    ObjectNode newAct = activityItems.addObject();
    newAct.put("type", "object");
    ObjectNode outerActivityProperties = newAct.putObject("properties")
        .putObject(activityTypeName);
    outerActivityProperties.put("$ref", "#/definitions/basic-activity-inner");
    // we could even add the custom activities field
    newAct.putArray("required").add(activityTypeName);
  }

  private static String camelToKebabCase(String str) {
    return str.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
  }

}
