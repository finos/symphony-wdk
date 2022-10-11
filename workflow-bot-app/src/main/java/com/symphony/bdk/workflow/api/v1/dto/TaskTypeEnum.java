package com.symphony.bdk.workflow.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.Arrays;

@AllArgsConstructor
@ToString
public enum TaskTypeEnum {
  // Events
  FORM_REPLIED_EVENT("FormRepliedEvent"),
  ACTIVITY_EXPIRED_EVENT("ActivityExpiredEvent"),
  ACTIVITY_COMPLETED_EVENT("ActivityCompletedEvent"),
  ACTIVITY_FAILED_EVENT("ActivityFailedEvent"),
  MESSAGE_RECEIVED_EVENT("MessageReceivedEvent"),
  MESSAGE_SUPPRESSED_EVENT("MessageSuppressedEvent"),
  POST_SHARED_EVENT("PostSharedEvent"),
  IM_CREATED_EVENT("ImCreatedEvent"),
  ROOM_CREATED_EVENT("RoomCreatedEvent"),
  ROOM_UPDATED_EVENT("RoomUpdatedEvent"),
  ROOM_DEACTIVATED_EVENT("RoomDeactivatedEvent"),
  ROOM_REACTIVATED_EVENT("RoomReactivatedEvent"),
  ROOM_MEMBER_PROMOTED_TO_OWNER_EVENT("RoomMemberPromotedToOwnerEvent"),
  ROOM_MEMBER_DEMOTED_FROM_OWNER_EVENT("RoomMemberDemotedFromOwnerEvent"),
  USER_JOINED_ROOM_EVENT("UserJoinedRoomEvent"),
  USER_LEFT_ROOM_EVENT("UserLeftRoomEvent"),
  USER_REQUESTED_TO_JOIN_ROOM_EVENT("UserRequestedToJoinRoomEvent"),
  CONNECTION_REQUESTED_EVENT("ConnectionRequestedEvent"),
  CONNECTION_ACCEPTED_EVENT("ConnectionAcceptedEvent"),
  TIMER_FIRED_EVENT("TimerFiredEvent"),
  REQUEST_RECEIVED_EVENT("RequestReceivedEvent"),

  // Activities
  GET_ATTACHMENT_ACTIVITY("GetAttachment"),
  ACCEPT_CONNECTION_ACTIVITY("AcceptConnection"),
  CREATE_CONNECTION_ACTIVITY("CreateConnection"),
  GET_CONNECTION_ACTIVITY("GetConnection"),
  GET_CONNECTIONS_ACTIVITY("GetConnections"),
  REJECT_CONNECTION_ACTIVITY("RejectConnection"),
  REMOVE_CONNECTION_ACTIVITY("GetConnection"),
  ADD_GROUP_MEMBER_ACTIVITY("AddGroupMember"),
  CREATE_GROUP_ACTIVITY("CreateGroup"),
  GET_GROUP_ACTIVITY("GetGroup"),
  GET_GROUPS_ACTIVITY("GetGroups"),
  UPDATE_GROUP_ACTIVITY("UpdateGroup"),
  GET_MESSAGE_ACTIVITY("GetMessage"),
  GET_MESSAGES_ACTIVITY("GetMessages"),
  PIN_MESSAGE_ACTIVITY("PinMessage"),
  SEND_MESSAGE_ACTIVITY("SendMessage"),
  UNPIN_MESSAGE_ACTIVITY("UnpinMessage"),
  UPDATE_MESSAGE_ACTIVITY("UpdateMessage"),
  EXECUTE_REQUEST_ACTIVITY("ExecuteRequest"),
  ADD_ROOM_MEMBER_ACTIVITY("AddRoomMember"),
  CREATE_ROOM_ACTIVITY("CreateRoom"),
  DEMOTE_ROOM_OWNER_ACTIVITY("DemoteRoomOwner"),
  GET_ROOM_ACTIVITY("GetRoom"),
  GET_ROOM_MEMBERS_ACTIVITY("GetRoomMembers"),
  GET_ROOMS_ACTIVITY("GetRooms"),
  PROMOTE_ROOM_MEMBER_ACTIVITY("PromoteRoomMember"),
  REMOVE_ROOM_MEMBER_ACTIVITY("RemoveRoomMember"),
  UPDATE_ROOM_ACTIVITY("UpdateRoom"),
  GET_STREAM_ACTIVITY("GetStream"),
  GET_STREAM_MEMBERS_ACTIVITY("GetStreamMembers"),
  GET_STREAMS_ACTIVITY("GetStreams"),
  GET_USER_STREAMS_ACTIVITY("GetUserStreams"),
  ADD_USER_ROLE_ACTIVITY("AddUserRole"),
  CREATE_SYSTEM_USER_ACTIVITY("CreateSystemUser"),
  CREATE_USER_ACTIVITY("CreateUser"),
  GET_USER_ACTIVITY("GetUser"),
  GET_USERS_ACTIVITY("GetUsers"),
  REMOVE_USER_ROLE_ACTIVITY("RemoveUserRole"),
  UPDATE_SYSTEM_USER_ACTIVITY("UpdateSystemUser"),
  UPDATE_USER_ACTIVITY("UpdateUser"),
  EXECUTE_SCRIPT_ACTIVITY("ExecuteScript");

  private final String text;

  private final static String EVENT = "EVENT";
  private final static String ACTIVITY = "ACTIVITY";

  public static TaskTypeEnum findByAbbr(final String abbr) {
    return Arrays.stream(values()).filter(value -> value.text.equals(abbr)).findFirst().orElse(null);
  }

  public String toType() {
    if (name().endsWith("_EVENT")) {
      return name().substring(0, name().length() - 6);
    }
    return name().substring(0, name().length() - 9);
  }

  public String toGroup() {
    if (name().endsWith("_EVENT")) {
      return EVENT;
    }
    return ACTIVITY;
  }
}
