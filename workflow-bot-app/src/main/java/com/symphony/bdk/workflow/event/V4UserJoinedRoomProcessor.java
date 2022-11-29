package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4UserJoinedRoom;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4UserJoinedRoomProcessor extends AbstractRealTimeEventProcessor<V4UserJoinedRoom> {

  public V4UserJoinedRoomProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.USER_JOINED_ROOM.getEventName());
  }
}
