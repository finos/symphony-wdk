package com.symphony.bdk.workflow.engine.executor.message;

import static com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor.OUTPUT_MESSAGE_ID_KEY;
import static com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor.OUTPUT_MESSAGE_KEY;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.message.UpdateMessage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class UpdateMessageExecutor implements ActivityExecutor<UpdateMessage> {

  @Override
  public void execute(ActivityExecutorContext<UpdateMessage> execution) throws IOException {
    String messageId = execution.getActivity().getMessageId();
    String content = extractContent(execution);
    Message message = Message.builder().content(content).build();
    V4Message updatedMessage = execution.bdk().messages().update(messageToUpdate, message);

    Map<String, Object> outputs = new HashMap<>();
    outputs.put(OUTPUT_MESSAGE_KEY, updatedMessage);
    outputs.put(OUTPUT_MESSAGE_ID_KEY, updatedMessage.getMessageId());
    execution.setOutputVariables(outputs);
  }

  private static String extractContent(ActivityExecutorContext<UpdateMessage> execution) throws IOException {
    if(execution.getActivity().getContent() != null) {
      return execution.getActivity().getContent();
    } else {
      String template = execution.getActivity().getTemplate();
      File file = execution.getResourceFile(Path.of(template));
      return execution.bdk().messages().templates().newTemplateFromFile(file.getPath()).process(execution.getVariables());
    }
  }
}
