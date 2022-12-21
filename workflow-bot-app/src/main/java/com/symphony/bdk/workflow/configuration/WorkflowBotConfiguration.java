package com.symphony.bdk.workflow.configuration;

import com.symphony.bdk.ext.group.SymphonyGroupBdkExtension;
import com.symphony.bdk.workflow.engine.ResourceProvider;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@EnableCaching
@Getter
@Profile("!test")
public class WorkflowBotConfiguration {

  @Value("${wdk.workflows.path:./workflows}")
  private String workflowsFolderPath;

  @Value("${wdk.properties.monitoring-token:}")
  private String monitoringToken;

  @Value("${wdk.properties.management-token:}")
  private String managementToken;

  @Bean("workflowResourcesProvider")
  public ResourceProvider workflowResourcesProvider() {
    // the folder is used both to load workflows and local resources
    return new WorkflowResourcesProvider(this.workflowsFolderPath);
  }

  @Bean
  public SymphonyGroupBdkExtension groupExtension() {
    return new SymphonyGroupBdkExtension();
  }

  @Bean(name = {"dataSource"})
  @Primary
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSource dataSource() {
    return DataSourceBuilder.create().build();
  }

  /*@Bean
  @Primary
  @ConfigurationProperties(prefix="spring.datasource")
  public DataSource primaryDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  @ConfigurationProperties(prefix="spring.camundadatasource")
  public DataSource secondaryDataSource() {
    return DataSourceBuilder.create().build();
  }*/
}
