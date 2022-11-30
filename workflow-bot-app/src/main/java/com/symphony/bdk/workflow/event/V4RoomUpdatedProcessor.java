package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4RoomUpdated;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4RoomUpdatedProcessor extends AbstractRealTimeEventProcessor<V4RoomUpdated> {

  public V4RoomUpdatedProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.ROOM_UPDATED.getEventName());
  }
}
