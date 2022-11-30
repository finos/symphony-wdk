package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4UserRequestedToJoinRoom;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4UserRequestedToJoinRoomProcessor extends AbstractRealTimeEventProcessor<V4UserRequestedToJoinRoom> {

  public V4UserRequestedToJoinRoomProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.USER_REQUESTED_JOIN_ROOM.getEventName());
  }
}
