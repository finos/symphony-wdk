package com.symphony.bdk.workflow.validators;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

/**
 * This class validates Yaml workflow content.
 * It provides methods to validate a Yaml file and Yaml content as a string.
 */
public class YamlValidator {

  private static final Logger logger = LoggerFactory.getLogger(YamlValidator.class);
  private static final String JSON_SCHEMA_FILE= "json-schema.json";
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
  private static final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

  private YamlValidator() {}

  public static ProcessingReport validateYamlFile(String yamlPath) throws IOException, ProcessingException {
    final JsonNode schemaJson = objectMapper.readTree(
        loadResourceWithClassPath(JSON_SCHEMA_FILE));
    final JsonNode yamlProposalOne = convertYamlFileToJsonNode(yamlPath);

    return validate(yamlProposalOne, schemaJson);
  }

  public static ProcessingReport validateYamlString(String yamlString) throws IOException, ProcessingException {
    final JsonNode schemaJson = objectMapper.readTree(
        loadResourceWithClassPath(JSON_SCHEMA_FILE));
    final JsonNode yamlProposalOne = convertYamlStringToJsonNode(yamlString);

    return validate(yamlProposalOne, schemaJson);
  }

  /**
   * This method validates a {@link JsonNode} against a json schema
   * @param jsonNode: json to be validated
   * @param jsonSchema: schema to use for validation
   * @return Report with success/failure status
   */
  private static ProcessingReport validate(JsonNode jsonNode, JsonNode jsonSchema)
      throws ProcessingException {
    final JsonSchema schema = factory.getJsonSchema(jsonSchema);
    ProcessingReport report = schema.validate(jsonNode);

    if (report.isSuccess()) {
      logger.info("Yaml file VALID");
    } else {
      logger.info("Yaml file NOT VALID");
    }

    return report;
  }

  /**
   * This method loads file using class path
   * @param filename: relative path to the resource file
   */
  private static File loadResourceWithClassPath(String filename) throws IOException {
    return new ClassPathResource(filename).getFile();
  }

  /**
   * This method converts yaml file to {@link JsonNode}
   * @param yamlFilename: relative path to yaml file
   */
  private static JsonNode convertYamlFileToJsonNode(String yamlFilename) throws IOException {
    Object obj = yamlReader.readValue(
        loadResourceWithClassPath(yamlFilename), Object.class);
    return writeJsonNode(obj);
  }

  /**
   * This method converts yaml string to {@link JsonNode}
   * @param yamlString: Yaml content as a string
   */
  private static JsonNode convertYamlStringToJsonNode(String yamlString) throws IOException {
    return writeJsonNode(yamlReader.readValue(yamlString, Object.class));

  }

  /**
   * This method creates {@link JsonNode} from an {@link Object}
   */
  private static JsonNode writeJsonNode(Object obj)
      throws JsonProcessingException {
    String jsonString = objectMapper.writeValueAsString(obj);
    return objectMapper.readTree(jsonString);
  }

}
