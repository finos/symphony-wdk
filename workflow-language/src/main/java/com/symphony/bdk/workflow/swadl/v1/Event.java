package com.symphony.bdk.workflow.swadl.v1;

import com.symphony.bdk.workflow.swadl.v1.event.ActivityCompletedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ActivityExpiredEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ActivityFailedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ConnectionAcceptedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ConnectionRequestedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.FormRepliedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ImCreatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.MessageReceivedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.MessageSuppressedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.PostSharedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RequestReceivedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomCreatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomDeactivatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomMemberDemotedFromOwnerEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomMemberPromotedToOwnerEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomReactivatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomUpdatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.TimerFiredEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserJoinedRoomEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserLeftRoomEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserRequestedToJoinRoomEvent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
  @JsonProperty
  private List<Event> oneOf;

  @JsonProperty
  private List<Event> allOf;

  @JsonProperty
  private FormRepliedEvent formReplied;

  @JsonProperty
  private ActivityExpiredEvent activityExpired;

  @JsonProperty
  private ActivityCompletedEvent activityCompleted;

  @JsonProperty
  private ActivityFailedEvent activityFailed;

  @JsonProperty
  private MessageReceivedEvent messageReceived;

  @JsonProperty
  private MessageSuppressedEvent messageSuppressed;

  @JsonProperty
  private PostSharedEvent postShared;

  @JsonProperty
  private ImCreatedEvent imCreated;

  @JsonProperty
  private RoomCreatedEvent roomCreated;

  @JsonProperty
  private RoomUpdatedEvent roomUpdated;

  @JsonProperty
  private RoomDeactivatedEvent roomDeactivated;

  @JsonProperty
  private RoomReactivatedEvent roomReactivated;

  @JsonProperty
  private RoomMemberPromotedToOwnerEvent roomMemberPromotedToOwner;

  @JsonProperty
  private RoomMemberDemotedFromOwnerEvent roomMemberDemotedFromOwner;

  @JsonProperty
  private UserJoinedRoomEvent userJoinedRoom;

  @JsonProperty
  private UserLeftRoomEvent userLeftRoom;

  @JsonProperty
  private UserRequestedToJoinRoomEvent userRequestedJoinRoom;

  @JsonProperty
  private ConnectionRequestedEvent connectionRequested;

  @JsonProperty
  private ConnectionAcceptedEvent connectionAccepted;

  @JsonProperty
  private TimerFiredEvent timerFired;

  @JsonProperty
  private RequestReceivedEvent requestReceived;

}
