package com.symphony.bdk.workflow.engine.executor;


import com.symphony.bdk.core.service.stream.util.StreamUtil;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.workflow.swadl.v1.activity.SendMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SendMessageExecutor implements ActivityExecutor<SendMessage> {

  public static final String OUTPUT_MESSAGE_ID_KEY = "msgId";

  @Override
  public void execute(ActivityExecutorContext<SendMessage> execution) {
    SendMessage activity = execution.getActivity();
    String streamId = resolveStreamId(execution, activity);
    log.debug("Running activity {} to send message to room {}", activity.getId(), streamId);

    if (streamId.endsWith("=")) { // TODO should be done in the BDK
      streamId = StreamUtil.toUrlSafeStreamId(streamId);
    }
    V4Message message = execution.messages().send(streamId, activity.getContent());

    execution.setOutputVariable(OUTPUT_MESSAGE_ID_KEY, message.getMessageId());
  }

  private String resolveStreamId(ActivityExecutorContext<SendMessage> execution, SendMessage activity) {
    if (activity.getTo() != null && activity.getTo().getStreamId() != null) {
      // either set explicitly in the workflow
      return activity.getTo().getStreamId();

      // or retrieved from the current event
    } else if (execution.getEvent() != null
        && execution.getEvent().getSource() instanceof V4MessageSent) {
      V4MessageSent event = (V4MessageSent) execution.getEvent().getSource();
      return event.getMessage().getStream().getStreamId();

    } else if (execution.getEvent() != null
        && execution.getEvent().getSource() instanceof V4SymphonyElementsAction) {
      V4SymphonyElementsAction event = (V4SymphonyElementsAction) execution.getEvent().getSource();
      return event.getStream().getStreamId();

    } else {
      throw new IllegalArgumentException("No stream id set to send a message");
    }
  }
}
