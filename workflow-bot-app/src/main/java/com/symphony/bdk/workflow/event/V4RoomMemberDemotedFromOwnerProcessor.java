package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4RoomMemberDemotedFromOwner;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4RoomMemberDemotedFromOwnerProcessor
    extends AbstractRealTimeEventProcessor<V4RoomMemberDemotedFromOwner> {

  public V4RoomMemberDemotedFromOwnerProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.ROOM_MEMBER_DEMOTED_FROM_OWNER.getEventName());
  }
}
