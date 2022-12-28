package com.symphony.bdk.workflow.engine.executor.message;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.engine.camunda.UtilityFunctionsMapper;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.message.UpdateMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor.OUTPUT_MESSAGE_ID_KEY;
import static com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor.OUTPUT_MESSAGE_KEY;

@Slf4j
public class UpdateMessageExecutor implements ActivityExecutor<UpdateMessage> {

  @Override
  public void execute(ActivityExecutorContext<UpdateMessage> execution) throws IOException {
    String messageId = execution.getActivity().getMessageId();
    log.debug("Updating message [{}]", messageId);
    V4Message messageToUpdate = execution.bdk().messages().getMessage(messageId);
    String content = extractContent(execution);
    Boolean silent = execution.getActivity().getSilent();
    log.debug("Updating message silently ? [{}]", silent);
    Message message = Message.builder().silent(silent).content(content).build();
    V4Message updatedMessage = execution.bdk().messages().update(messageToUpdate, message);

    Map<String, Object> outputs = new HashMap<>();
    outputs.put(OUTPUT_MESSAGE_KEY, updatedMessage);
    outputs.put(OUTPUT_MESSAGE_ID_KEY, updatedMessage.getMessageId());
    execution.setOutputVariables(outputs);
  }

  private static String extractContent(ActivityExecutorContext<UpdateMessage> execution) throws IOException {
    if (execution.getActivity().getContent() != null) {
      return execution.getActivity().getContent();
    } else {
      String template = execution.getActivity().getTemplate();
      File file = execution.getResourceFile(Path.of(template));
      Map<String, Object> templateVariables = new HashMap<>(execution.getVariables());
      // also bind our utility functions, so they can be used inside templates
      templateVariables.put(UtilityFunctionsMapper.WDK_PREFIX, new UtilityFunctionsMapper(execution.bdk().session()));
      return execution.bdk()
          .messages()
          .templates()
          .newTemplateFromFile(file.getPath())
          .process(templateVariables);
    }
  }
}
