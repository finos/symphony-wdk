package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.gen.api.model.RoomTag;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.workflow.swadl.v1.activity.room.EditRoom;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class EditRoomExecutor implements ActivityExecutor<EditRoom> {

  private static final String OUTPUT_ROOM_KEY = "room";

  @Override
  public void execute(ActivityExecutorContext<EditRoom> execution) {
    EditRoom editRoom = execution.getActivity();

    if (shouldUpdateRoom(editRoom)) {
      log.debug("Updating room {} attributes", editRoom.getStreamId());
      V3RoomAttributes attributes = toAttributes(editRoom);
      execution.streams().updateRoom(editRoom.getStreamId(), attributes);
    }

    if (editRoom.getActive() != null) {
      // this is a different API call but we support it in the same activity
      log.debug("Updating room {} active status", editRoom.getStreamId());
      execution.streams().setRoomActive(editRoom.getStreamId(), editRoom.getActive());
    }

    // services called above return different results and might end up not being called so we explicitly call the API
    // to return the same info in all cases
    V3RoomDetail updatedRoom = execution.streams().getRoomInfo(editRoom.getStreamId());

    execution.setOutputVariable(OUTPUT_ROOM_KEY, updatedRoom);
  }

  private boolean shouldUpdateRoom(EditRoom editRoom) {
    // active is a different API call so it is handled separately and not checked here
    return editRoom.getRoomName() != null
        || editRoom.getRoomDescription() != null
        || editRoom.getKeywords() != null
        || editRoom.getMembersCanInvite() != null
        || editRoom.getDiscoverable() != null
        || editRoom.getIsPublic() != null
        || editRoom.getReadOnly() != null
        || editRoom.getCopyProtected() != null
        || editRoom.getCrossPod() != null
        || editRoom.getViewHistory() != null
        || editRoom.getMultiLateralRoom() != null;
  }

  private V3RoomAttributes toAttributes(EditRoom editRoom) {
    V3RoomAttributes attributes = new V3RoomAttributes();
    attributes.setName(editRoom.getRoomName());
    attributes.setDescription(editRoom.getRoomDescription());

    if (editRoom.getKeywords() != null) {
      List<RoomTag> tags = editRoom.getKeywords().entrySet().stream()
          .map(e -> {
            RoomTag tag = new RoomTag();
            tag.setKey(e.getKey());
            tag.setValue(e.getValue());
            return tag;
          })
          .collect(Collectors.toList());
      attributes.setKeywords(tags);
    }

    attributes.membersCanInvite(editRoom.getMembersCanInvite());
    attributes.setDiscoverable(editRoom.getDiscoverable());
    attributes.setPublic(editRoom.getIsPublic());
    attributes.setReadOnly(editRoom.getReadOnly());
    attributes.copyProtected(editRoom.getCopyProtected());
    attributes.crossPod(editRoom.getCrossPod());
    attributes.viewHistory(editRoom.getViewHistory());
    attributes.multiLateralRoom(editRoom.getMultiLateralRoom());

    return attributes;
  }

}
