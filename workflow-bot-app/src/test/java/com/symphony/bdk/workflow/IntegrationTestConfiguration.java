package com.symphony.bdk.workflow;

import com.symphony.bdk.workflow.engine.ResourceProvider;
import com.symphony.bdk.workflow.engine.camunda.UtilityFunctionsMapper;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.el.ExpressionManager;
import org.camunda.bpm.engine.impl.scripting.ExecutableScript;
import org.camunda.bpm.engine.impl.scripting.env.ScriptingEnvironment;
import org.camunda.bpm.engine.impl.util.ReflectUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.nio.file.Paths;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.sql.DataSource;

@TestConfiguration
@EnableJpaRepositories
@PropertySource("application-test.yaml")
@Slf4j
@EnableTransactionManagement
@Profile("test")
public class IntegrationTestConfiguration implements ProcessEnginePlugin {
  @Bean("workflowResourcesProvider")
  public ResourceProvider workflowResourcesProvider() {
    return new TestResourcesProvider(Paths.get("dummy").toString());
  }

  @Bean(name = "camundaBpmDataSource")
  @ConfigurationProperties(prefix = "spring.camundadatasource")
  public DataSource camundaDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();
    expressionManager.addFunction(UtilityFunctionsMapper.TEXT,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctionsMapper.TEXT, String.class));
    expressionManager.addFunction(UtilityFunctionsMapper.JSON,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctionsMapper.JSON, String.class));
    expressionManager.addFunction(UtilityFunctionsMapper.ESCAPE,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctionsMapper.ESCAPE, String.class));
    expressionManager.addFunction(UtilityFunctionsMapper.MENTIONS,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctionsMapper.MENTIONS, Object.class));
    expressionManager.addFunction(UtilityFunctionsMapper.HASHTAGS,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctionsMapper.HASHTAGS, Object.class));
    expressionManager.addFunction(UtilityFunctionsMapper.CASHTAGS,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctionsMapper.CASHTAGS, Object.class));
    expressionManager.addFunction(UtilityFunctionsMapper.EMOJIS,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctionsMapper.EMOJIS, Object.class));
    expressionManager.addFunction(UtilityFunctionsMapper.SESSION,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctionsMapper.SESSION));
  }

  @Override
  public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    handleScriptExceptionsAsBpmnErrors(processEngineConfiguration);
  }

  // By default, script exceptions (except for BPMNError) are not failing the script task
  // We change this behavior to wrap any script exception in a BpmnError to handle errors with activity-failed
  private void handleScriptExceptionsAsBpmnErrors(ProcessEngineConfigurationImpl processEngineConfiguration) {
    ScriptingEnvironment scriptingEnvironment = processEngineConfiguration.getScriptingEnvironment();
    processEngineConfiguration.setScriptingEnvironment(new ScriptingEnvironment(null, null, null) {
      @Override
      public Object execute(ExecutableScript script, VariableScope scope) {
        try {
          return scriptingEnvironment.execute(script, scope);
        } catch (Exception e) {
          log.error("Failed to execute script", e);
          throw new BpmnError("FAILURE", e);
        }
      }

      @Override
      public Object execute(ExecutableScript script, VariableScope scope, Bindings bindings,
          ScriptEngine scriptEngine) {
        try {
          return scriptingEnvironment.execute(script, scope, bindings, scriptEngine);
        } catch (Exception e) {
          log.error("Failed to execute script", e);
          throw new BpmnError("FAILURE", e);
        }
      }
    });
  }

  @Override
  public void postProcessEngineBuild(ProcessEngine processEngine) {

  }
}
