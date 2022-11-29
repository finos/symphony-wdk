package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4RoomReactivated;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4RoomReactivatedProcessor extends AbstractRealTimeEventProcessor<V4RoomReactivated> {

  public V4RoomReactivatedProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.ROOM_REACTIVATED.getEventName());
  }
}
