package com.symphony.bdk.workflow.lang;


import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.validator.YamlValidator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.camunda.commons.utils.IoUtil;

import java.io.IOException;
import java.io.InputStream;

public class WorkflowBuilder {

  private WorkflowBuilder() {

  }

  public static Workflow fromYaml(InputStream yaml) throws IOException, ProcessingException {
    // TODO remove camunda dependency
    String yamlString = IoUtil.inputStreamAsString(yaml);
    YamlValidator.validateYamlString(yamlString);

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true));
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    return mapper.readValue(yamlString, Workflow.class);
  }
}
