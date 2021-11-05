package com.symphony.bdk.workflow.engine.executor.message;

import static com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor.OUTPUT_MESSAGE_ID_KEY;
import static com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor.OUTPUT_MESSAGE_KEY;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.message.UpdateMessage;

import java.io.IOException;

public class UpdateMessageExecutor implements ActivityExecutor<UpdateMessage> {

  @Override
  public void execute(ActivityExecutorContext<UpdateMessage> execution) throws IOException {
    String messageId = execution.getActivity().getMessageId();
    V4Message messageToUpdate =  execution.bdk().messages().getMessage(messageId);
    Message message = Message.builder().content(execution.getActivity().getContent()).build();
    V4Message updatedMessage = execution.bdk().messages().update(messageToUpdate, message);

    execution.setOutputVariable(OUTPUT_MESSAGE_KEY, updatedMessage);
    execution.setOutputVariable(OUTPUT_MESSAGE_ID_KEY, updatedMessage.getMessageId());
  }
}
