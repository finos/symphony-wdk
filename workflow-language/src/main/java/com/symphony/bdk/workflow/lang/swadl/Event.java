package com.symphony.bdk.workflow.lang.swadl;

import com.symphony.bdk.workflow.lang.swadl.event.ActivityExpiredEvent;
import com.symphony.bdk.workflow.lang.swadl.event.ActivityFinishedEvent;
import com.symphony.bdk.workflow.lang.swadl.event.ConnectionAcceptedEvent;
import com.symphony.bdk.workflow.lang.swadl.event.ConnectionRequestedEvent;
import com.symphony.bdk.workflow.lang.swadl.event.FormRepliedEvent;
import com.symphony.bdk.workflow.lang.swadl.event.ImCreatedEvent;
import com.symphony.bdk.workflow.lang.swadl.event.MessageReceivedEvent;
import com.symphony.bdk.workflow.lang.swadl.event.MessageSuppressedEvent;
import com.symphony.bdk.workflow.lang.swadl.event.PostSharedEvent;
import com.symphony.bdk.workflow.lang.swadl.event.RoomCreatedEvent;
import com.symphony.bdk.workflow.lang.swadl.event.RoomDeactivatedEvent;
import com.symphony.bdk.workflow.lang.swadl.event.RoomMemberPromotedToOwnerEvent;
import com.symphony.bdk.workflow.lang.swadl.event.RoomReactivatedEvent;
import com.symphony.bdk.workflow.lang.swadl.event.RoomUpdatedEvent;
import com.symphony.bdk.workflow.lang.swadl.event.UserJoinedRoomEvent;
import com.symphony.bdk.workflow.lang.swadl.event.UserLeftRoomEvent;
import com.symphony.bdk.workflow.lang.swadl.event.UserRequestedToJoinRoomEvent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
  private String timeout = "PT5S";

  private List<Event> oneOf;

  private FormRepliedEvent formReplied;

  private ActivityExpiredEvent activityExpired;

  private ActivityFinishedEvent activityFinished;

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
}
