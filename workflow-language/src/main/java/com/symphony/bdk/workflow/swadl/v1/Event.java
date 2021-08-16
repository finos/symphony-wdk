package com.symphony.bdk.workflow.swadl.v1;

import com.symphony.bdk.workflow.swadl.v1.event.ActivityCompletedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ActivityExpiredEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ConnectionAcceptedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ConnectionRequestedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.FormRepliedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ImCreatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.MessageReceivedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.MessageSuppressedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.PostSharedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomCreatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomDeactivatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomMemberPromotedToOwnerEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomReactivatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomUpdatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.TimerFiredEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserJoinedRoomEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserLeftRoomEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserRequestedToJoinRoomEvent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
  private String timeout = "PT30S";

  private List<Event> oneOf;

  private FormRepliedEvent formReplied;

  private ActivityExpiredEvent activityExpired;

  private ActivityCompletedEvent activityCompleted;

  private MessageReceivedEvent messageReceived;

  private MessageSuppressedEvent messageSuppressed;

  private PostSharedEvent postShared;

  private ImCreatedEvent imCreated;

  private RoomCreatedEvent roomCreated;

  private RoomUpdatedEvent roomUpdated;

  private RoomDeactivatedEvent roomDeactivated;

  private RoomReactivatedEvent roomReactivated;

  private RoomMemberPromotedToOwnerEvent roomMemberPromotedToOwner;

  private RoomMemberPromotedToOwnerEvent roomMemberDemotedFromOwner;

  private UserJoinedRoomEvent userJoinedRoom;

  private UserLeftRoomEvent userLeftRoom;

  private UserRequestedToJoinRoomEvent userRequestedJoinRoom;

  private ConnectionRequestedEvent connectionRequested;

  private ConnectionAcceptedEvent connectionAccepted;

  private TimerFiredEvent timerFired;
}
