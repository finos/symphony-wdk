package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4UserLeftRoom;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4UserLeftRoomProcessor extends AbstractRealTimeEventProcessor<V4UserLeftRoom> {

  public V4UserLeftRoomProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.USER_LEFT_ROOM.getEventName());
  }
}
