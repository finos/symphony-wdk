package com.symphony.bdk.workflow.swadl;


import com.symphony.bdk.workflow.swadl.v1.Activity;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.validator.YamlValidator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WorkflowBuilder {

  public static final ObjectMapper MAPPER = new ObjectMapper(
      new YAMLFactory().configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true));

  static {
    MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Activity.class, new ActivityDeserializer());
    MAPPER.registerModule(module);
  }

  private WorkflowBuilder() {
  }

  public static Workflow fromYaml(InputStream yaml) throws IOException, ProcessingException {
    String yamlString = IOUtils.toString(yaml, StandardCharsets.UTF_8);
    YamlValidator.validateYaml(yamlString);
    return MAPPER.readValue(yamlString, Workflow.class);
  }
}
