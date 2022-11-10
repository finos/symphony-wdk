package com.symphony.bdk.workflow.swadl.validator;

import com.symphony.bdk.workflow.swadl.ActivityRegistry;
import com.symphony.bdk.workflow.swadl.exception.SwadlNotValidException;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Validates a SWADL workflow written in YAML.
 */
@Slf4j
public class SwadlValidator {

  private static final String JSON_SCHEMA_FILE = "/swadl-schema-1.0.json";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final ObjectMapper YAML_READER = new ObjectMapper(new YAMLFactory());
  private static final JsonSchemaFactory JSON_SCHEMA_FACTORY = JsonSchemaFactory.newBuilder()
      // filter out warnings such as custom x-documentation properties
      .setReportProvider(new ListReportProvider(LogLevel.ERROR, LogLevel.FATAL))
      .freeze();

  private static final JsonNode jsonSchema;

  static {
    // load it only once as it won't change dynamically (i.e. we don't support adding new custom activities on the fly)
    try (InputStream schemaStream = SwadlValidator.class.getResourceAsStream(JSON_SCHEMA_FILE)) {
      if (schemaStream == null) {
        throw new IOException("Could not read JSON schema from classpath location: " + JSON_SCHEMA_FILE);
      }
      jsonSchema = OBJECT_MAPPER.readTree(schemaStream);
      addCustomActivitiesToSchema(jsonSchema);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load JSON schema", e);
    }
  }

  private SwadlValidator() {
  }

  public static void validateYaml(String yaml) throws SwadlNotValidException, ProcessingException {
    validate(yaml);
  }

  private static void validate(String yaml) throws ProcessingException, SwadlNotValidException {

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

  /**
   * On the fly, we add the custom activities discovered in the classpath to the JSON Schema.This way we can validate
   * them at least for basic attributes.
   */
  private static void addCustomActivitiesToSchema(JsonNode jsonSchema) {
    // we expect activities to be defined this way in the JSON schema
    ObjectNode activityItems = (ObjectNode) jsonSchema.get("properties").get("activities").get("items");

    // remove the pattern that allows to define any custom activities and replace it with the known custom activities
    activityItems.remove("patternProperties");

    // builtin activities are already in JSON Schema
    Iterable<String> iterable = () -> activityItems.get("properties").fieldNames();
    Set<String> alreadyDefinedActivities = StreamSupport
        .stream(iterable.spliterator(), false)
        .collect(Collectors.toSet());

    for (Class<? extends BaseActivity> activityType : ActivityRegistry.getActivityTypes()) {
      // in YAML, activities are referenced like my-activity and the class is named MyActivity
      String activityTypeName = camelToKebabCase(activityType.getSimpleName());
      if (!alreadyDefinedActivities.contains(activityTypeName)) {
        addJsonSchemaForCustomActivity((ObjectNode) activityItems.get("properties"), activityTypeName);
        // avoid defining an activity twice, however deserialization checks for duplicates and will fail
        alreadyDefinedActivities.add(activityTypeName);
      }
    }
  }

  private static void addJsonSchemaForCustomActivity(ObjectNode activityItems, String activityTypeName) {
    ObjectNode newAct = activityItems.putObject(activityTypeName);
    // we just use the activity name and set the basic fields
    newAct.put("$ref", "#/definitions/basic-activity-inner");
    // we could even try to add its fields using reflection
  }

  private static String camelToKebabCase(String str) {
    return str.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
  }

}
