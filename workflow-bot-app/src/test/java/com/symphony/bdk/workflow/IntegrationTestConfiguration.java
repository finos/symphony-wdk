package com.symphony.bdk.workflow;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.configuration.WorkflowBotConfiguration;
import com.symphony.bdk.workflow.engine.ResourceProvider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.nio.file.Paths;

@TestConfiguration
@Slf4j
@Profile("test")
public class IntegrationTestConfiguration {
  @Bean("workflowResourcesProvider")
  public ResourceProvider workflowResourcesProvider() {
    return new TestResourcesProvider(Paths.get("dummy").toString());
  }

  @Bean(name = "workflowBotConfiguration")
  public WorkflowBotConfiguration workflowBotConfiguration() {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn("MONITORING_TOKEN_VALUE");
    when(workflowBotConfiguration.getManagementToken()).thenReturn("MANAGEMENT_TOKEN_VALUE");
    when(workflowBotConfiguration.getWorkflowsFolderPath()).thenReturn("./");
    return workflowBotConfiguration;
  }
}
