package com.symphony.bdk.workflow.engine.camunda.executor;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CreateRoomExecutor implements JavaDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateRoomExecutor.class);
  private static final String OUTPUT_ROOM_ID_KEY = "roomId";
  
  private final StreamService streamService;
  private final MessageService messageService;

  public CreateRoomExecutor(StreamService streamService, MessageService messageService) {
    this.streamService = streamService;
    this.messageService = messageService;
  }

  @Override
  public void execute(DelegateExecution execution) {
    String uidsAsString = (String) execution.getVariable("uids");
    List<Long> uids = new ArrayList<>();
    if (!StringUtils.isEmpty(uidsAsString)) {
      List<String> uidsStringList = Arrays.asList(
          ((String) execution.getVariable("uids"))
              .split(","));

      uids = uidsStringList.stream().map(Long::valueOf).collect(Collectors.toList());
    }

    String name = (String) execution.getVariable("name");
    String description = (String) execution.getVariable("description");
    boolean isPublic = Boolean.parseBoolean((String) execution.getVariable("public"));

    final String createdRoomId;
    if (!uids.isEmpty()) {
      Stream stream = this.streamService.create(uids);
      createdRoomId = stream.getId();
      this.messageService.send(stream.getId(), "Welcome on board!");
      LOGGER.info("MIM created with {} users, id={}", uids.size(), createdRoomId);
    } else { // at least name should be set
      V3RoomAttributes v3RoomAttributes = new V3RoomAttributes();
      v3RoomAttributes.setName(name);
      v3RoomAttributes.setPublic(isPublic);
      v3RoomAttributes.setDescription(description);
      V3RoomDetail v3RoomDetail = this.streamService.create(v3RoomAttributes);
      createdRoomId = v3RoomDetail.getRoomSystemInfo().getId();
      LOGGER.info("Stream {} created, id={}", name, createdRoomId);
    }

    execution.setVariable(OUTPUT_ROOM_ID_KEY, createdRoomId);
  }
}
