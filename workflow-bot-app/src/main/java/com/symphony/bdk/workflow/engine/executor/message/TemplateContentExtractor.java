package com.symphony.bdk.workflow.engine.executor.message;

import com.symphony.bdk.workflow.engine.camunda.UtilityFunctionsMapper;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TemplateContentExtractor {

  public static String extractContent(ActivityExecutorContext<?> execution, String content, String templatePath,
      String template)
      throws IOException {
    if (content != null) {
      return content;
    } else {
      Map<String, Object> templateVariables = new HashMap<>(execution.getVariables());
      // also bind our utility functions so they can be used inside templates
      templateVariables.put(UtilityFunctionsMapper.WDK_PREFIX,
          new UtilityFunctionsMapper(execution.bdk().session(), execution.sharedDataStore(), execution.secretKeeper()));

      if (templatePath != null) {
        File file = execution.getResourceFile(Path.of(templatePath));
        return execution.bdk()
            .messages()
            .templates()
            .newTemplateFromFile(file.getPath())
            .process(templateVariables);
      } else {
        return execution.bdk()
            .messages()
            .templates()
            .newTemplateFromString(template)
            .process(templateVariables);
      }
    }
  }
}
