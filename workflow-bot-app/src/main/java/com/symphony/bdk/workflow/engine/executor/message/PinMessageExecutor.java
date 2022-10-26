package com.symphony.bdk.workflow.engine.executor.message;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.gen.api.model.StreamType;
import com.symphony.bdk.gen.api.model.V1IMAttributes;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.message.PinMessage;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;

import java.io.IOException;

@Slf4j
public class PinMessageExecutor extends OboExecutor<PinMessage, Void>
    implements ActivityExecutor<PinMessage> {

  public static final String IM = StreamType.TypeEnum.IM.getValue();
  public static final String ROOM = StreamType.TypeEnum.ROOM.getValue();

  protected PinMessageExecutor(RuntimeService runtimeService) {
    super(runtimeService);
  }

  @Override
  public void execute(ActivityExecutorContext<PinMessage> execution) throws IOException {
    PinMessage activity = execution.getActivity();
    String messageId = execution.getActivity().getMessageId();

    V4Message messageToPin = execution.bdk().messages().getMessage(messageId);
    String streamId = messageToPin.getStream().getStreamId();
    String streamType = messageToPin.getStream().getStreamType();

    if (IM.equals(streamType) && this.isObo(activity)) {
      throw new IllegalArgumentException(
          String.format("Pin instant message, in activity %s, is not OBO enabled", activity.getId()));
    } else if (IM.equals(streamType)) {
      this.updateInstantMessage(execution, messageId, streamId);
    } else if (ROOM.equals(streamType) && this.isObo(activity)) {
      this.doOboWithCache(execution);
    } else if (ROOM.equals(streamType)) {
      this.updateRoomMessage(execution, messageId, streamId);
    } else {
      throw new IllegalArgumentException(
          String.format("Unable to pin message in stream type %s in activity %s", streamType,
              execution.getActivity().getId()));
    }
  }

  @Override
  protected Void doOboWithCache(ActivityExecutorContext<PinMessage> execution) {
    PinMessage activity = execution.getActivity();
    V4Message messageToPin = execution.bdk().messages().getMessage(activity.getMessageId());
    String streamId = messageToPin.getStream().getStreamId();
    V3RoomAttributes roomAttributes = new V3RoomAttributes();
    roomAttributes.setPinnedMessageId(activity.getMessageId());
    AuthSession authSession = this.getOboAuthSession(execution);

    log.debug("Pin message {} in Room {} with OBO", activity.getMessageId(), streamId);
    execution.bdk().obo(authSession).streams().updateRoom(streamId, roomAttributes);
    return null;
  }

  private void updateInstantMessage(ActivityExecutorContext<PinMessage> execution, String messageId, String streamId) {
    V1IMAttributes imAttributes = new V1IMAttributes();
    imAttributes.setPinnedMessageId(messageId);

    log.debug("Pin message {} in IM {}", messageId, streamId);
    execution.bdk().streams().updateInstantMessage(streamId, imAttributes);
  }

  private void updateRoomMessage(ActivityExecutorContext<PinMessage> execution, String messageId, String streamId) {
    V3RoomAttributes roomAttributes = new V3RoomAttributes();
    roomAttributes.setPinnedMessageId(messageId);

    log.debug("Pin message {} in Room {}", messageId, streamId);
    execution.bdk().streams().updateRoom(streamId, roomAttributes);
  }
}
