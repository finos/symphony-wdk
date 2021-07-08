package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CreateRoomExecutor implements ActivityExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateRoomExecutor.class);
  private static final String OUTPUT_ROOM_ID_KEY = "roomId";

  @Override
  public void execute(ActivityExecutorContext execution) {
    String uidsAsString = execution.getVariable("uids");
    List<Long> uids = new ArrayList<>();
    if (!StringUtils.isEmpty(uidsAsString)) {
      List<String> uidsStringList = Arrays.asList(
          execution.getVariable("uids")
              .split(","));

      uids = uidsStringList.stream().map(Long::valueOf).collect(Collectors.toList());
    }

    String name = execution.getVariable("name");
    String description = execution.getVariable("description");
    boolean isPublic = Boolean.parseBoolean(execution.getVariable("public"));

    final String createdRoomId;

    if (!uids.isEmpty() && !StringUtils.isEmpty(name) && !StringUtils.isEmpty(description)) {
      createdRoomId = this.createStream(execution, uids, name, description, isPublic);
      LOGGER.info("Stream {} created with {} users, id={}", name, uids.size(), createdRoomId);
    } else if (!uids.isEmpty()) {
      createdRoomId = this.createStream(execution, uids);
      LOGGER.info("MIM created with {} users, id={}", uids.size(), createdRoomId);
    } else {
      createdRoomId = this.createStream(execution, name, description, isPublic);
      LOGGER.info("Stream {} created, id={}", name, createdRoomId);
    }

    execution.setVariable(OUTPUT_ROOM_ID_KEY, createdRoomId);
  }

  private String createStream(ActivityExecutorContext execution, List<Long> uids) {
    Stream stream = execution.streams().create(uids);
    return stream.getId();
  }

  private String createStream(ActivityExecutorContext execution, String name, String description, boolean isPublic) {
    V3RoomAttributes v3RoomAttributes = new V3RoomAttributes();
    v3RoomAttributes.name(name).description(description)._public(isPublic);
    V3RoomDetail v3RoomDetail = execution.streams().create(v3RoomAttributes);
    return v3RoomDetail.getRoomSystemInfo().getId();
  }

  private String createStream(ActivityExecutorContext execution, List<Long> uids, String name, String description,
      boolean isPublic) {
    String streamId = createStream(execution, name, description, isPublic);
    uids.forEach(uid -> execution.streams().addMemberToRoom(uid, streamId));
    return streamId;
  }
}
