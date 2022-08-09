package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.auth.AuthSession;
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
    final boolean isObo = this.isObo(updateRoom);
    AuthSession authSession = null;

    if (shouldUpdateRoom(updateRoom)) {
      log.debug("Updating room {} attributes", updateRoom.getStreamId());
      V3RoomAttributes attributes = toAttributes(updateRoom);
      if (isObo) {
        authSession = this.getOboAuthSession(execution, updateRoom);
        execution.bdk().obo(authSession).streams().updateRoom(updateRoom.getStreamId(), attributes);
      } else {
        execution.bdk().streams().updateRoom(updateRoom.getStreamId(), attributes);
      }
    }

    if (updateRoom.getActive() != null) {
      if (isObo) {
        throw new IllegalArgumentException(
            String.format("Room active status update, in activity %s, is not OBO enabled", updateRoom.getId()));
      } else {
        // this is a different API call, but we support it in the same activity
        log.debug("Updating room {} active status", updateRoom.getStreamId());
        execution.bdk().streams().setRoomActive(updateRoom.getStreamId(), updateRoom.getActive());
      }
    }

    // services called above return different results and might end up not being called so we explicitly call the API
    // to return the same info in all cases
    final V3RoomDetail updatedRoom;
    if (isObo) {
      updatedRoom = execution.bdk().obo(authSession).streams().getRoomInfo(updateRoom.getStreamId());
    } else {
      updatedRoom = execution.bdk().streams().getRoomInfo(updateRoom.getStreamId());
    }

    execution.setOutputVariable(OUTPUT_ROOM_KEY, updatedRoom);
  }

  private boolean isObo(UpdateRoom updateRoom) {
    return updateRoom.getObo() != null && (updateRoom.getObo().getUsername() != null
        || updateRoom.getObo().getUserId() != null);
  }

  private AuthSession getOboAuthSession(ActivityExecutorContext<UpdateRoom> execution, UpdateRoom updateRoomActivity) {
    if (updateRoomActivity.getObo().getUsername() != null) {
      return execution.bdk().obo(updateRoomActivity.getObo().getUsername());
    } else {
      return execution.bdk().obo(updateRoomActivity.getObo().getUserId());
    }
  }

  private boolean shouldUpdateRoom(UpdateRoom updateRoom) {
    // active is a different API call so it is handled separately and not checked here
    return updateRoom.getRoomName() != null
        || updateRoom.getRoomDescription() != null
        || updateRoom.getKeywords() != null
        || updateRoom.getMembersCanInvite() != null
        || updateRoom.getDiscoverable() != null
        || updateRoom.getIsPublic() != null
        || updateRoom.getReadOnly() != null
        || updateRoom.getCopyProtected() != null
        || updateRoom.getCrossPod() != null
        || updateRoom.getViewHistory() != null
        || updateRoom.getMultilateralRoom() != null;
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

    attributes.membersCanInvite(updateRoom.getMembersCanInvite());
    attributes.setDiscoverable(updateRoom.getDiscoverable());
    attributes.setPublic(updateRoom.getIsPublic());
    attributes.setReadOnly(updateRoom.getReadOnly());
    attributes.copyProtected(updateRoom.getCopyProtected());
    attributes.crossPod(updateRoom.getCrossPod());
    attributes.viewHistory(updateRoom.getViewHistory());
    attributes.multiLateralRoom(updateRoom.getMultilateralRoom());

    return attributes;
  }

}
