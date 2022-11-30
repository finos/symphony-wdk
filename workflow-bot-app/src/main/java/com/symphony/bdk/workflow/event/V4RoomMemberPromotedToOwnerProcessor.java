package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4RoomMemberPromotedToOwner;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4RoomMemberPromotedToOwnerProcessor extends AbstractRealTimeEventProcessor<V4RoomMemberPromotedToOwner> {

  public V4RoomMemberPromotedToOwnerProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.ROOM_MEMBER_PROMOTED_TO_OWNER.getEventName());
  }
}
