package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4RoomDeactivated;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4RoomDeactivatedProcessor extends AbstractRealTimeEventProcessor<V4RoomDeactivated> {

  public V4RoomDeactivatedProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.ROOM_DEACTIVATED.getEventName());
  }
}
