package com.symphony.bdk.workflow.lang.swadl;

import com.symphony.bdk.workflow.lang.swadl.event.ActivityExpiredEvent;
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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
  private String timeout = "PT5S";

  @JsonProperty("form-replied")
  private FormRepliedEvent formReplied;

  @JsonProperty("activity-expired")
  private ActivityExpiredEvent activityExpired;

  @JsonProperty("message-received")
  private MessageReceivedEvent messageReceived;

  @JsonProperty("message-suppressed")
  private MessageSuppressedEvent messageSuppressed;

  @JsonProperty("post-shared")
  private PostSharedEvent postShared;

  @JsonProperty("im-created")
  private ImCreatedEvent imCreated;

  @JsonProperty("room-created")
  private RoomCreatedEvent roomCreated;

  @JsonProperty("room-updated")
  private RoomUpdatedEvent roomUpdated;

  @JsonProperty("room-deactivated")
  private RoomDeactivatedEvent roomDeactivated;

  @JsonProperty("room-reactivated")
  private RoomReactivatedEvent roomReactived;

  @JsonProperty("room-member-promoted-to-owner")
  private RoomMemberPromotedToOwnerEvent roomMemberPromotedToOwner;

  @JsonProperty("room-member-demoted-from-owner")
  private RoomMemberPromotedToOwnerEvent roomMemberDemotedFromOwner;

  @JsonProperty("user-joined-room")
  private UserJoinedRoomEvent userJoinedRoom;

  @JsonProperty("user-left-room")
  private UserLeftRoomEvent userLeftRoom;

  @JsonProperty("user-requested-join-room")
  private UserRequestedToJoinRoomEvent userRequestedJoinRoom;

  @JsonProperty("connection-requested")
  private ConnectionRequestedEvent connectionRequested;

  @JsonProperty("connection-accepted")
  private ConnectionAcceptedEvent connectionAccepted;
}
