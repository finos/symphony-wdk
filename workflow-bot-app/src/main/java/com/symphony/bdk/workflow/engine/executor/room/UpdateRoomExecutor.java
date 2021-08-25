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
      execution.streams().updateRoom(updateRoom.getStreamId(), attributes);
    }

    if (updateRoom.getActiveAsBool() != null) {
      // this is a different API call but we support it in the same activity
      log.debug("Updating room {} active status", updateRoom.getStreamId());
      execution.streams().setRoomActive(updateRoom.getStreamId(), updateRoom.getActiveAsBool());
    }

    // services called above return different results and might end up not being called so we explicitly call the API
    // to return the same info in all cases
    V3RoomDetail updatedRoom = execution.streams().getRoomInfo(updateRoom.getStreamId());

    execution.setOutputVariable(OUTPUT_ROOM_KEY, updatedRoom);
  }

  private boolean shouldUpdateRoom(UpdateRoom updateRoom) {
    // active is a different API call so it is handled separately and not checked here
    return updateRoom.getRoomName() != null
        || updateRoom.getRoomDescription() != null
        || updateRoom.getKeywords() != null
        || updateRoom.getMembersCanInviteAsBool() != null
        || updateRoom.getDiscoverableAsBool() != null
        || updateRoom.getIsPublicAsBool() != null
        || updateRoom.getReadOnlyAsBool() != null
        || updateRoom.getCopyProtectedAsBool() != null
        || updateRoom.getCrossPodAsBool() != null
        || updateRoom.getViewHistoryAsBool() != null
        || updateRoom.getMultiLateralRoomAsBool() != null;
  }

  private V3RoomAttributes toAttributes(UpdateRoom updateRoom) {
    V3RoomAttributes attributes = new V3RoomAttributes();
    attributes.setName(updateRoom.getRoomName());
    attributes.setDescription(updateRoom.getRoomDescription());

    if (updateRoom.getKeywords() != null) {
      List<RoomTag> tags = updateRoom.getKeywords().entrySet().stream()
          .map(e -> {
            RoomTag tag = new RoomTag();
            tag.setKey(e.getKey());
            tag.setValue(e.getValue());
            return tag;
          })
          .collect(Collectors.toList());
      attributes.setKeywords(tags);
    }

    attributes.membersCanInvite(updateRoom.getMembersCanInviteAsBool());
    attributes.setDiscoverable(updateRoom.getDiscoverableAsBool());
    attributes.setPublic(updateRoom.getIsPublicAsBool());
    attributes.setReadOnly(updateRoom.getReadOnlyAsBool());
    attributes.copyProtected(updateRoom.getCopyProtectedAsBool());
    attributes.crossPod(updateRoom.getCrossPodAsBool());
    attributes.viewHistory(updateRoom.getViewHistoryAsBool());
    attributes.multiLateralRoom(updateRoom.getMultiLateralRoomAsBool());

    return attributes;
  }

}
