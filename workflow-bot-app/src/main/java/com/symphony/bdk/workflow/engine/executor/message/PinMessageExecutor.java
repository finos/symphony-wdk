package com.symphony.bdk.workflow.engine.executor.message;

import com.symphony.bdk.core.auth.AuthSession;
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
      this.updateInstantMessage(execution, messageId, streamId);
    } else if (ROOM.equals(streamType)) {
      this.updateRoomMessage(execution, messageId, streamId);
    } else {
      throw new IllegalArgumentException(
          String.format("Unable to pin message in stream type %s in activity %s", streamType,
              execution.getActivity().getId()));
    }
  }

  private boolean isObo(PinMessage activity) {
    return activity.getObo() != null && (activity.getObo().getUsername() != null
        || activity.getObo().getUserId() != null);
  }

  private void updateInstantMessage(ActivityExecutorContext<PinMessage> execution, String messageId, String streamId) {
    PinMessage activity = execution.getActivity();
    V1IMAttributes imAttributes = new V1IMAttributes();
    imAttributes.setPinnedMessageId(messageId);

    log.debug("Pin message {} in IM {}", messageId, streamId);

    if (this.isObo(activity)) {
      throw new IllegalArgumentException(
          String.format("Pin instant message, in activity %s, is not OBO enabled", activity.getId()));
    } else {
      execution.bdk().streams().updateInstantMessage(streamId, imAttributes);
    }
  }

  private void updateRoomMessage(ActivityExecutorContext<PinMessage> execution, String messageId, String streamId) {
    PinMessage activity = execution.getActivity();
    V3RoomAttributes roomAttributes = new V3RoomAttributes();
    roomAttributes.setPinnedMessageId(messageId);

    log.debug("Pin message {} in Room {}", messageId, streamId);

    if (this.isObo(activity)) {
      AuthSession authSession;
      if (activity.getObo().getUsername() != null) {
        authSession = execution.bdk().obo(activity.getObo().getUsername());
      } else {
        authSession = execution.bdk().obo(activity.getObo().getUserId());
      }

      execution.bdk().obo(authSession).streams().updateRoom(streamId, roomAttributes);
    } else {
      execution.bdk().streams().updateRoom(streamId, roomAttributes);
    }
  }
}
