package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4ConnectionAccepted;
import com.symphony.bdk.gen.api.model.V4ConnectionRequested;
import com.symphony.bdk.gen.api.model.V4InstantMessageCreated;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.gen.api.model.V4MessageSuppressed;
import com.symphony.bdk.gen.api.model.V4RoomCreated;
import com.symphony.bdk.gen.api.model.V4RoomDeactivated;
import com.symphony.bdk.gen.api.model.V4RoomMemberDemotedFromOwner;
import com.symphony.bdk.gen.api.model.V4RoomMemberPromotedToOwner;
import com.symphony.bdk.gen.api.model.V4RoomReactivated;
import com.symphony.bdk.gen.api.model.V4RoomUpdated;
import com.symphony.bdk.gen.api.model.V4SharedPost;
import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.gen.api.model.V4UserJoinedRoom;
import com.symphony.bdk.gen.api.model.V4UserLeftRoom;
import com.symphony.bdk.gen.api.model.V4UserRequestedToJoinRoom;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.WorkflowEngine;

import lombok.Generated;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Entry points for Datafeed events, they are dispatched to the workflow engine from there.
 */
@Component
@Generated // not interesting to test
@SuppressWarnings("unchecked")
public class DatafeedEventToWorkflowEvent {

  private final WorkflowEngine workflowEngine;

  public DatafeedEventToWorkflowEvent(WorkflowEngine workflowEngine) {
    this.workflowEngine = workflowEngine;
  }

  @EventListener
  public void onMessageSent(RealTimeEvent<? extends V4MessageSent> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onSymphonyElementsAction(RealTimeEvent<? extends V4SymphonyElementsAction> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onConnectionRequested(RealTimeEvent<? extends V4ConnectionRequested> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onConnectionAccepted(RealTimeEvent<? extends V4ConnectionAccepted> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onMessageSuppressed(RealTimeEvent<? extends V4MessageSuppressed> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onSharedPost(RealTimeEvent<? extends V4SharedPost> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onInstantMessageCreated(RealTimeEvent<? extends V4InstantMessageCreated> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onRoomCreated(RealTimeEvent<? extends V4RoomCreated> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onRoomUpdated(RealTimeEvent<? extends V4RoomUpdated> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onRoomDeactivated(RealTimeEvent<? extends V4RoomDeactivated> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onRoomReactivated(RealTimeEvent<? extends V4RoomReactivated> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onUserRequestedToJoinRoom(RealTimeEvent<? extends V4UserRequestedToJoinRoom> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onUserJoinedRoom(RealTimeEvent<? extends V4UserJoinedRoom> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onUserLeftRoom(RealTimeEvent<? extends V4UserLeftRoom> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onRoomMemberPromotedToOwner(RealTimeEvent<? extends V4RoomMemberPromotedToOwner> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onRoomMemberDemotedFromOwner(RealTimeEvent<? extends V4RoomMemberDemotedFromOwner> event) {
    workflowEngine.onEvent(event);
  }
}
