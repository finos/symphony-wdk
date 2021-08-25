package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.CreateRoom;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Slf4j
public class CreateRoomExecutor implements ActivityExecutor<CreateRoom> {

  private static final String OUTPUT_ROOM_ID_KEY = "roomId";

  @Override
  public void execute(ActivityExecutorContext<CreateRoom> execution) {
    CreateRoom activity = execution.getActivity();
    List<Long> uids = activity.getUuidsAsLongs();
    String name = activity.getRoomName();
    String description = activity.getRoomDescription();
    boolean isPublic = activity.isPublicAsBool();

    final String createdRoomId;

    if (uids != null && !uids.isEmpty() && !StringUtils.isEmpty(name) && !StringUtils.isEmpty(description)) {
      createdRoomId = this.createRoom(execution, uids, name, description, isPublic);
      log.debug("Stream {} created with {} users, id={}", name, uids.size(), createdRoomId);
    } else if (uids != null && !uids.isEmpty()) {
      createdRoomId = this.createRoom(execution, uids);
      log.debug("MIM created with {} users, id={}", uids.size(), createdRoomId);
    } else {
      createdRoomId = this.createRoom(execution, name, description, isPublic);
      log.debug("Stream {} created, id={}", name, createdRoomId);
    }

    execution.setOutputVariable(OUTPUT_ROOM_ID_KEY, createdRoomId);
  }

  private String createRoom(ActivityExecutorContext execution, List<Long> uids) {
    Stream stream = execution.streams().create(uids);
    return stream.getId();
  }

  private String createRoom(ActivityExecutorContext execution, String name, String description, boolean isPublic) {
    V3RoomAttributes v3RoomAttributes = new V3RoomAttributes();
    v3RoomAttributes.name(name).description(description)._public(isPublic);
    V3RoomDetail v3RoomDetail = execution.streams().create(v3RoomAttributes);
    return v3RoomDetail.getRoomSystemInfo().getId();
  }

  private String createRoom(ActivityExecutorContext execution, List<Long> uids, String name, String description,
      boolean isPublic) {
    String roomId = createRoom(execution, name, description, isPublic);
    uids.forEach(uid -> execution.streams().addMemberToRoom(uid, roomId));
    return roomId;
  }
}
