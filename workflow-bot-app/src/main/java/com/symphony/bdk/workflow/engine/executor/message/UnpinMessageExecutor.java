package com.symphony.bdk.workflow.engine.executor.message;

import static com.symphony.bdk.workflow.engine.executor.message.PinMessageExecutor.IM;
import static com.symphony.bdk.workflow.engine.executor.message.PinMessageExecutor.ROOM;

import com.symphony.bdk.core.util.IdUtil;
import com.symphony.bdk.gen.api.model.V1IMAttributes;
import com.symphony.bdk.gen.api.model.V2StreamAttributes;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.message.UnpinMessage;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class UnpinMessageExecutor implements ActivityExecutor<UnpinMessage> {

  @Override
  public void execute(ActivityExecutorContext<UnpinMessage> execution) throws IOException {
    String streamId = IdUtil.toUrlSafeIdIfNeeded(execution.getActivity().getStreamId());

    V2StreamAttributes stream = execution.bdk().streams().getStream(streamId);
    String streamType = stream.getStreamType().getType();

    if (IM.equals(streamType)) {

      V1IMAttributes imAttributes = new V1IMAttributes();
      imAttributes.setPinnedMessageId("");

      log.debug("Unpin message in IM {}", streamId);
      execution.bdk().streams().updateInstantMessage(streamId, imAttributes);
    } else if (ROOM.equals(streamType)) {

      V3RoomAttributes roomAttributes = new V3RoomAttributes();
      roomAttributes.setPinnedMessageId("");

      log.debug("Unpin message in Room {}", streamId);
      execution.bdk().streams().updateRoom(streamId, roomAttributes);
    } else {
      log.warn("Unable to unpin message in stream {} with type {}", streamId, streamType);
    }
  }
}
