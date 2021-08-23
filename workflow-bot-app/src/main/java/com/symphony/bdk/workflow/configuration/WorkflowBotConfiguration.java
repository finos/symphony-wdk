package com.symphony.bdk.workflow.configuration;

import com.symphony.bdk.workflow.engine.ResourceProvider;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.FileInputStream;

@Configuration
@Profile("!test")
public class WorkflowBotConfiguration {

  @Bean("workflowResourcesProvider")
  public ResourceProvider workflowResourcesProvider(@Value("${workflows.folder}") String resourcesFolder) {
    return path -> new FileInputStream(StringUtils.appendIfMissing(resourcesFolder, "/") + path);
  }
}
