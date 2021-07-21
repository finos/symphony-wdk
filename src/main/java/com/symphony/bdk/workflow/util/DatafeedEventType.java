package com.symphony.bdk.workflow.util;

import java.util.Arrays;
import java.util.Optional;

public enum DatafeedEventType {
  ROOM_CREATED("V4RoomCreated"), ROOM_UPDATED("V4RoomUpdated"),
  ROOM_DEACTIVATED("V4RoomDeactivated"), ROOM_REACTIVATED("V4RoomReactivated"),
  USER_REQUESTED_TO_JOIN_ROOM("V4UserRequestedToJoinRoom"), USER_JOINED_ROOM("V4UserJoinedRoom"),
  USER_LEFT_ROOM("V4UserLeftRoom"), ROOM_MEMBER_PROMOTED_TO_OWNER("V4RoomMemberPromotedToOwner"),
  ROOM_MEMBER_DEMOTED_FROM_OWNER("V4RoomMemberDemotedFromOwner"), CONNECTION_ACCEPTED("V4ConnectionAccepted"),
  CONNECTION_REQUESTED("V4ConnectionRequested"), MESSAGE_SUPPRESSED("V4MessageSuppressed"),
  MESSAGE_SENT("V4MessageSent"), INSTANT_MESSAGE_CREATED("V4InstantMessageCreated"),
  SHARED_POST("V4SharedPost"), NO_EVENT("NoEvent");

  String typeName;

  DatafeedEventType(String typeName) {
    this.typeName = typeName;
  }

  public String getTypeName() {
    return typeName;
  }

  public static DatafeedEventType fromValue(String value) {
    Optional<DatafeedEventType> event = Arrays.stream(values())
        .filter(bl -> bl.typeName.equalsIgnoreCase(value))
        .findFirst();

    return event.isPresent() ? event.get() : NO_EVENT;
  }
}
