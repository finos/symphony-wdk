package com.symphony.bdk.workflow.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowBotConfiguration {

  @Bean
  @Qualifier("yamlObjectMapper")
  public ObjectMapper yamlObjectMapper() {
    return new ObjectMapper(new YAMLFactory()).configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }
  
}
