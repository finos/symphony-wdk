package com.symphony.bdk.workflow.engine.executor.message;

import static com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor.OUTPUT_MESSAGES_KEY;
import static com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor.OUTPUT_MESSAGE_IDS_KEY;
import static com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor.OUTPUT_MESSAGE_ID_KEY;
import static com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor.OUTPUT_MESSAGE_KEY;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.message.UpdateMessage;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    outputs.put(OUTPUT_MESSAGES_KEY, updatedMessage);
    outputs.put(OUTPUT_MESSAGE_ID_KEY, updatedMessage.getMessageId());
    List<String> msgIds = new ArrayList<>();
    msgIds.add(updatedMessage.getMessageId());
    outputs.put(OUTPUT_MESSAGE_IDS_KEY, msgIds);
    execution.setOutputVariables(outputs);
  }

  private static String extractContent(ActivityExecutorContext<UpdateMessage> execution) throws IOException {
    UpdateMessage activity = execution.getActivity();
    return TemplateContentExtractor.extractContent(execution, activity.getContent(), activity.getTemplatePath(),
        activity.getTemplate());
  }
}
