package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.workflow.engine.executor.BdkGateway;

import lombok.RequiredArgsConstructor;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.script.Bindings;
import javax.script.ScriptEngine;

@Configuration
@RequiredArgsConstructor
@Order(Ordering.DEFAULT_ORDER + 1)
@Slf4j
public class CamundaEngineConfiguration implements ProcessEnginePlugin {

  private final BdkGateway bdkGateway;

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
    processEngineConfiguration.getBeans().put(
        UtilityFunctionsMapper.WDK_PREFIX, new UtilityFunctionsMapper(this.bdkGateway.session()));
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
