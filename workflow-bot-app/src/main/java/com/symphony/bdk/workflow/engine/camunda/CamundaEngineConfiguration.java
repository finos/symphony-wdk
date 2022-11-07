package com.symphony.bdk.workflow.engine.camunda;

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
import org.camunda.bpm.spring.boot.starter.configuration.Ordering;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.sql.DataSource;

@Configuration
@Order(Ordering.DEFAULT_ORDER + 1)
@Slf4j
public class CamundaEngineConfiguration implements ProcessEnginePlugin {

  @Bean
  public PlatformTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();
    expressionManager.addFunction(UtilityFunctions.TEXT,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctions.TEXT, String.class));
    expressionManager.addFunction(UtilityFunctions.JSON,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctions.JSON, String.class));
    expressionManager.addFunction(UtilityFunctions.ESCAPE,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctions.ESCAPE, String.class));
    expressionManager.addFunction(UtilityFunctions.MENTIONS,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctions.MENTIONS, Object.class));
    expressionManager.addFunction(UtilityFunctions.HASHTAGS,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctions.HASHTAGS, Object.class));
    expressionManager.addFunction(UtilityFunctions.CASHTAGS,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctions.CASHTAGS, Object.class));
    expressionManager.addFunction(UtilityFunctions.EMOJIS,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, UtilityFunctions.EMOJIS, Object.class));
  }

  @Override
  public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    processEngineConfiguration.getBeans().put(UtilityFunctionsMapper.NAME, new UtilityFunctionsMapper());
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
