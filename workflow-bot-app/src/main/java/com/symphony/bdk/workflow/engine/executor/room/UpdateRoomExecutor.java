package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.gen.api.model.RoomTag;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.UpdateRoom;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class UpdateRoomExecutor implements ActivityExecutor<UpdateRoom> {

  private static final String OUTPUT_ROOM_KEY = "room";

  @Override
  public void execute(ActivityExecutorContext<UpdateRoom> execution) {
    UpdateRoom updateRoom = execution.getActivity();

    if (shouldUpdateRoom(updateRoom)) {
      log.debug("Updating room {} attributes", updateRoom.getStreamId());
      V3RoomAttributes attributes = toAttributes(updateRoom);
      execution.bdk().streams().updateRoom(updateRoom.getStreamId(), attributes);
    }

    if (updateRoom.getActive() != null) {
      // this is a different API call but we support it in the same activity
      log.debug("Updating room {} active status", updateRoom.getStreamId());
      execution.bdk().streams().setRoomActive(updateRoom.getStreamId(), updateRoom.getActive().get());
    }

    // services called above return different results and might end up not being called so we explicitly call the API
    // to return the same info in all cases
    V3RoomDetail updatedRoom = execution.bdk().streams().getRoomInfo(updateRoom.getStreamId());

    execution.setOutputVariable(OUTPUT_ROOM_KEY, updatedRoom);
  }

  private boolean shouldUpdateRoom(UpdateRoom updateRoom) {
    // active is a different API call so it is handled separately and not checked here
    return updateRoom.getRoomName() != null
        || updateRoom.getRoomDescription() != null
        || updateRoom.getKeywords() != null
        || updateRoom.getMembersCanInvite().get() != null
        || updateRoom.getDiscoverable().get() != null
        || updateRoom.getIsPublic().get() != null
        || updateRoom.getReadOnly().get() != null
        || updateRoom.getCopyProtected().get() != null
        || updateRoom.getCrossPod().get() != null
        || updateRoom.getViewHistory().get() != null
        || updateRoom.getMultilateralRoom().get() != null;
  }

  private V3RoomAttributes toAttributes(UpdateRoom updateRoom) {
    V3RoomAttributes attributes = new V3RoomAttributes();
    attributes.setName(updateRoom.getRoomName());
    attributes.setDescription(updateRoom.getRoomDescription());

    if (updateRoom.getKeywords() != null) {
      List<RoomTag> tags = updateRoom.getKeywords().get().entrySet().stream()
          .map(e -> {
            RoomTag tag = new RoomTag();
            tag.setKey(e.getKey());
            tag.setValue(e.getValue());
            return tag;
          })
          .collect(Collectors.toList());
      attributes.setKeywords(tags);
    }

    attributes.membersCanInvite(updateRoom.getMembersCanInvite().get());
    attributes.setDiscoverable(updateRoom.getDiscoverable().get());
    attributes.setPublic(updateRoom.getIsPublic().get());
    attributes.setReadOnly(updateRoom.getReadOnly().get());
    attributes.copyProtected(updateRoom.getCopyProtected().get());
    attributes.crossPod(updateRoom.getCrossPod().get());
    attributes.viewHistory(updateRoom.getViewHistory().get());
    attributes.multiLateralRoom(updateRoom.getMultilateralRoom().get());

    return attributes;
  }

}
