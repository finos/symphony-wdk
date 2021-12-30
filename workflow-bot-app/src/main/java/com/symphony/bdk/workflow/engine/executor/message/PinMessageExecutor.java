package com.symphony.bdk.workflow.engine.executor.message;

import com.symphony.bdk.gen.api.model.StreamType;
import com.symphony.bdk.gen.api.model.V1IMAttributes;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.message.PinMessage;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class PinMessageExecutor implements ActivityExecutor<PinMessage> {

  public static final String IM = StreamType.TypeEnum.IM.getValue();
  public static final String ROOM = StreamType.TypeEnum.ROOM.getValue();

  @Override
  public void execute(ActivityExecutorContext<PinMessage> execution) throws IOException {
    String messageId = execution.getActivity().getMessageId();

    V4Message messageToPin = execution.bdk().messages().getMessage(messageId);
    String streamId = messageToPin.getStream().getStreamId();
    String streamType = messageToPin.getStream().getStreamType();

    if (IM.equals(streamType)) {
      V1IMAttributes imAttributes = new V1IMAttributes();
      imAttributes.setPinnedMessageId(messageId);

      log.debug("Pin message {} in IM {}", messageId, streamId);
      execution.bdk().streams().updateInstantMessage(streamId, imAttributes);
    } else if (ROOM.equals(streamType)) {
      V3RoomAttributes roomAttributes = new V3RoomAttributes();
      roomAttributes.setPinnedMessageId(messageId);

      log.debug("Pin message {} in Room {}", messageId, streamId);
      execution.bdk().streams().updateRoom(streamId, roomAttributes);
    } else {
      throw new IllegalArgumentException(
          String.format("Unable to pin message in stream type %s in activity %s", streamType,
              execution.getActivity().getId()));
    }
  }
}
