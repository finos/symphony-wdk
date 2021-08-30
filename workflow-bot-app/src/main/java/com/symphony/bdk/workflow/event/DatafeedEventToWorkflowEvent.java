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
public class DatafeedEventToWorkflowEvent {

  private final WorkflowEngine workflowEngine;

  public DatafeedEventToWorkflowEvent(WorkflowEngine workflowEngine) {
    this.workflowEngine = workflowEngine;
  }

  @EventListener
  public void onMessageSent(RealTimeEvent<V4MessageSent> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onSymphonyElementsAction(RealTimeEvent<V4SymphonyElementsAction> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onConnectionRequested(RealTimeEvent<V4ConnectionRequested> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onConnectionAccepted(RealTimeEvent<V4ConnectionAccepted> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onMessageSuppressed(RealTimeEvent<V4MessageSuppressed> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onSharedPost(RealTimeEvent<V4SharedPost> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onInstantMessageCreated(RealTimeEvent<V4InstantMessageCreated> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onRoomCreated(RealTimeEvent<V4RoomCreated> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onRoomUpdated(RealTimeEvent<V4RoomUpdated> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onRoomDeactivated(RealTimeEvent<V4RoomDeactivated> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onRoomReactivated(RealTimeEvent<V4RoomReactivated> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onUserRequestedToJoinRoom(RealTimeEvent<V4UserRequestedToJoinRoom> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onUserJoinedRoom(RealTimeEvent<V4UserJoinedRoom> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onUserLeftRoom(RealTimeEvent<V4UserLeftRoom> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onRoomMemberPromotedToOwner(RealTimeEvent<V4RoomMemberPromotedToOwner> event) {
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onRoomMemberDemotedFromOwner(RealTimeEvent<V4RoomMemberDemotedFromOwner> event) {
    workflowEngine.onEvent(event);
  }
}
