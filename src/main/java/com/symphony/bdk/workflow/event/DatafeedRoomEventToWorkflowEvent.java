package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4RoomCreated;
import com.symphony.bdk.gen.api.model.V4RoomDeactivated;
import com.symphony.bdk.gen.api.model.V4RoomMemberDemotedFromOwner;
import com.symphony.bdk.gen.api.model.V4RoomMemberPromotedToOwner;
import com.symphony.bdk.gen.api.model.V4RoomReactivated;
import com.symphony.bdk.gen.api.model.V4RoomUpdated;
import com.symphony.bdk.gen.api.model.V4UserJoinedRoom;
import com.symphony.bdk.gen.api.model.V4UserLeftRoom;
import com.symphony.bdk.gen.api.model.V4UserRequestedToJoinRoom;
import com.symphony.bdk.spring.events.RealTimeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatafeedRoomEventToWorkflowEvent extends DatafeedEventToWorkflowEvent {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatafeedRoomEventToWorkflowEvent.class);

  @EventListener
  public void onRoomCreated(RealTimeEvent<V4RoomCreated> event) {
    LOGGER.info("Triggered room created event {}", event.getSource().getStream().getStreamId());
    workflowEngine.roomCreated(event);
  }

  @EventListener
  public void onRoomUpdated(RealTimeEvent<V4RoomUpdated> event) {
    LOGGER.info("Triggered room updated event {}", event.getSource().getStream().getStreamId());
    workflowEngine.roomUpdated(event);
  }

  @EventListener
  public void onRoomDeactivated(RealTimeEvent<V4RoomDeactivated> event) {
    LOGGER.info("Triggered room deactivated event {}", event.getSource().getStream().getStreamId());
    workflowEngine.roomDeactivated(event);
  }

  @EventListener
  public void onRoomReactivated(RealTimeEvent<V4RoomReactivated> event) {
    LOGGER.info("Triggered room reactivated event {}", event.getSource().getStream().getStreamId());
    workflowEngine.roomReactivated(event);
  }

  @EventListener
  public void onUserRequestedToJoinRoom(RealTimeEvent<V4UserRequestedToJoinRoom> event) {
    LOGGER.info("Triggered user requested to join room event {}", event.getSource().getStream().getStreamId());
    workflowEngine.userRequestedToJoinRoom(event);
  }

  @EventListener
  public void onUserJoinedRoom(RealTimeEvent<V4UserJoinedRoom> event) {
    LOGGER.info("Triggered user joined room event {}", event.getSource().getStream().getStreamId());
    workflowEngine.userJoinedRoom(event);
  }

  @EventListener
  public void onUserLeftRoom(RealTimeEvent<V4UserLeftRoom> event) {
    LOGGER.info("Triggered user left room event {}", event.getSource().getStream().getStreamId());
    workflowEngine.userLeftRoom(event);
  }

  @EventListener
  public void onRoomMemberPromotedToOwner(RealTimeEvent<V4RoomMemberPromotedToOwner> event) {
    LOGGER.info("Triggered user promoted to room owner event {}", event.getSource().getStream().getStreamId());
    workflowEngine.roomMemberPromotedToOwner(event);
  }

  @EventListener
  public void onRoomMemberDemotedFromOwner(RealTimeEvent<V4RoomMemberDemotedFromOwner> event) {
    LOGGER.info("Triggered user demoted from room owner event {}", event.getSource().getStream().getStreamId());
    workflowEngine.roomMemberDemotedFromOwner(event);
  }
}
