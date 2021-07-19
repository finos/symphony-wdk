package com.symphony.bdk.workflow.lang;


import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.validator.YamlValidator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WorkflowBuilder {

  private static final ObjectMapper MAPPER = new ObjectMapper(
      new YAMLFactory().configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true));

  static {
    MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
  }

  private WorkflowBuilder() {
  }

  public static Workflow fromYaml(InputStream yaml) throws IOException, ProcessingException {
    String yamlString = IOUtils.toString(yaml, StandardCharsets.UTF_8);
    YamlValidator.validateYamlString(yamlString);
    return MAPPER.readValue(yamlString, Workflow.class);
  }
}
