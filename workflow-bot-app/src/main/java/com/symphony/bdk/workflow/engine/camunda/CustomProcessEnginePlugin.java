package com.symphony.bdk.workflow.engine.camunda;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.stereotype.Component;

/**
 * This plugin allows injecting {@link UtilityFunctionsMapper} bean context to be directly used
 * in Camunda Groovy scripting
 */
@Component
public class CustomProcessEnginePlugin extends AbstractProcessEnginePlugin {

  @Override
  public void postInit(ProcessEngineConfigurationImpl conf) {
    conf.getBeans().put("utilityFunctionsMapper", new UtilityFunctionsMapper());
  }
}
