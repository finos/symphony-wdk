package com.symphony.bdk.workflow.engine.executor;


import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.lang.swadl.activity.SendMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SendMessageExecutor implements ActivityExecutor<SendMessage> {

  public static final String OUTPUT_MESSAGE_ID_KEY = "msgId";

  @Override
  public void execute(ActivityExecutorContext<SendMessage> execution) {
    SendMessage activity = execution.getActivity();
    log.info("Running activity {} to send message to room {}", activity.getId(), activity.getTo().getStreamId());

    V4Message message = execution.messages().send(activity.getTo().getStreamId(), activity.getContent());

    execution.setOutputVariable(activity.getId(), OUTPUT_MESSAGE_ID_KEY, message.getMessageId());
  }
}
