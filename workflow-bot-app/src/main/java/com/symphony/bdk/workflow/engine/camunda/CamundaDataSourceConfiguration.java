package com.symphony.bdk.workflow.engine.camunda;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
@EnableTransactionManagement
@EnableJpaRepositories(transactionManagerRef = "camundaBpmTransactionManager")
@Profile("!test")
public class CamundaDataSourceConfiguration {

  @Bean("camundaBpmDataSource")
  @ConfigurationProperties("spring.datasource.camunda")
  public DataSource camundaDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "camundaBpmTransactionManager")
  public PlatformTransactionManager camundaTransactionManager(
      @Qualifier("camundaBpmDataSource") DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }
}
