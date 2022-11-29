package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4RoomCreated;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4RoomCreatedProcessor extends AbstractRealTimeEventProcessor<V4RoomCreated> {

  public V4RoomCreatedProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.ROOM_CREATED.getEventName());
  }
}
