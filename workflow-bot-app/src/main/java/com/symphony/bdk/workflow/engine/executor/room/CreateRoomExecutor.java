package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.gen.api.model.RoomTag;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.CreateRoom;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
public class CreateRoomExecutor implements ActivityExecutor<CreateRoom> {

  private static final String OUTPUT_ROOM_ID_KEY = "roomId";

  @Override
  public void execute(ActivityExecutorContext<CreateRoom> execution) {
    CreateRoom activity = execution.getActivity();
    List<Long> uids = activity.getUserIdsAsLongs();
    String name = activity.getRoomName();
    String description = activity.getRoomDescription();

    final String createdRoomId;

    if (uids != null && !uids.isEmpty() && !StringUtils.isEmpty(name) && !StringUtils.isEmpty(description)) {
      createdRoomId = this.createRoom(execution, uids, toRoomAttributes(activity));
      log.debug("Stream {} created with {} users, id={}", name, uids.size(), createdRoomId);
    } else if (uids != null && !uids.isEmpty()) {
      createdRoomId = this.createRoom(execution, uids);
      log.debug("MIM created with {} users, id={}", uids.size(), createdRoomId);
    } else {
      createdRoomId = this.createRoom(execution, toRoomAttributes(activity));
      log.debug("Stream {} created, id={}", name, createdRoomId);
    }

    execution.setOutputVariable(OUTPUT_ROOM_ID_KEY, createdRoomId);
  }

  private V3RoomAttributes toRoomAttributes(CreateRoom createRoom) {
    V3RoomAttributes v3RoomAttributes = new V3RoomAttributes()
        .name(createRoom.getRoomName())
        .description(createRoom.getRoomDescription())
        ._public(createRoom.getIsPublic())
        .viewHistory(createRoom.getViewHistory())
        .discoverable(createRoom.getDiscoverable())
        .readOnly(createRoom.getReadOnly())
        .copyProtected(createRoom.getCopyProtected())
        .crossPod(createRoom.getCrossPod())
        .multiLateralRoom(createRoom.getMultilateralRoom())
        .membersCanInvite(createRoom.getMembersCanInvite())
        .subType(createRoom.getSubType());

    if (createRoom.getKeywords() != null) {
      for (Map.Entry<String, String> entry : createRoom.getKeywords().entrySet()) {
        v3RoomAttributes.addKeywordsItem(
            new RoomTag()
                .key(entry.getKey())
                .value(entry.getValue()));
      }
    }

    return v3RoomAttributes;
  }

  private String createRoom(ActivityExecutorContext<CreateRoom> execution, List<Long> uids,
      V3RoomAttributes v3RoomAttributes) {
    String roomId = createRoom(execution, v3RoomAttributes);

    if (this.isObo(execution.getActivity())) {
      AuthSession authSession = this.getOboAuthSession(execution, execution.getActivity());
      uids.forEach(uid -> execution.bdk().obo(authSession).streams().addMemberToRoom(uid, roomId));
    } else {
      uids.forEach(uid -> execution.bdk().streams().addMemberToRoom(uid, roomId));
    }

    return roomId;
  }

  private String createRoom(ActivityExecutorContext<CreateRoom> execution, List<Long> uids) {
    Stream stream;
    CreateRoom createRoomActivity = execution.getActivity();

    if (this.isObo(createRoomActivity)) {
      AuthSession authSession = this.getOboAuthSession(execution, createRoomActivity);
      stream = execution.bdk().obo(authSession).streams().create(uids);
    } else {
      stream = execution.bdk().streams().create(uids);
    }
    return stream.getId();
  }

  private String createRoom(ActivityExecutorContext<CreateRoom> execution, V3RoomAttributes v3RoomAttributes) {
    CreateRoom createRoomActivity = execution.getActivity();
    V3RoomDetail v3RoomDetail;

    if (this.isObo(createRoomActivity)) {
      AuthSession authSession = this.getOboAuthSession(execution, createRoomActivity);
      v3RoomDetail = execution.bdk().obo(authSession).streams().create(v3RoomAttributes);
    } else {
      v3RoomDetail = execution.bdk().streams().create(v3RoomAttributes);
    }

    return v3RoomDetail.getRoomSystemInfo().getId();
  }

  private boolean isObo(CreateRoom createRoom) {
    return createRoom.getObo() != null && (createRoom.getObo().getUsername() != null
        || createRoom.getObo().getUserId() != null);
  }

  private AuthSession getOboAuthSession(ActivityExecutorContext<CreateRoom> execution, CreateRoom createRoomActivity) {
    if (createRoomActivity.getObo().getUsername() != null) {
      return execution.bdk().obo(createRoomActivity.getObo().getUsername());
    } else {
      return execution.bdk().obo(createRoomActivity.getObo().getUserId());
    }
  }

}
