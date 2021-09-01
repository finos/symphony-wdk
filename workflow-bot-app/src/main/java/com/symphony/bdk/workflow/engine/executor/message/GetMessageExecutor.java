package com.symphony.bdk.workflow.engine.executor.message;

import com.symphony.bdk.core.service.stream.util.StreamUtil;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.message.GetMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetMessageExecutor implements ActivityExecutor<GetMessage> {

  private static final String OUTPUT_MESSAGE_KEY = "message";

  @Override
  public void execute(ActivityExecutorContext<GetMessage> context) {
    String messageId = context.getActivity().getMessageId();

    // TODO remove once https://github.com/finos/symphony-bdk-java/pull/567 is released
    if (messageId.endsWith("=")) {
      messageId = StreamUtil.toUrlSafeStreamId(messageId);
    }

    log.debug("Get message {}", messageId);
    V4Message message = context.bdk().messages().getMessage(messageId);

    context.setOutputVariable(OUTPUT_MESSAGE_KEY, message);
  }
}
