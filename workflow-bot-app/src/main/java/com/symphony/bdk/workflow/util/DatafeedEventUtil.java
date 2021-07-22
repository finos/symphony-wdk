package com.symphony.bdk.workflow.util;

import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;

public class DatafeedEventUtil {

  public static <T> DatafeedEventType getEventType(T event) {
    String typeName = event.getClass().getSimpleName();

    return DatafeedEventType.fromValue(typeName);
  }

  public static <T> String buildMessageNameFrom(RealTimeEvent<T> event, DatafeedEventType eventType) {
    if (V4MessageSent.class.equals(event.getSource().getClass())) {
      // TODO presentation ML parsing
      return ((RealTimeEvent<V4MessageSent>) event).getSource().getMessage().getMessage();
//      case MESSAGE_SUPPRESSED:
//        return ((RealTimeEvent<V4MessageSuppressed>) event).getSource().getMessageId();
//      case INSTANT_MESSAGE_CREATED:
//        return ((RealTimeEvent<V4InstantMessageCreated>) event).getSource().getStream().getStreamId();
//      case SHARED_POST:
//        return ((RealTimeEvent<V4SharedPost>) event).getSource().getMessage().getMessage();
//      case CONNECTION_ACCEPTED:
//        return ((RealTimeEvent<V4ConnectionAccepted>) event).getInitiator().getUser().getUsername();
//      case CONNECTION_REQUESTED:
//        return ((RealTimeEvent<V4ConnectionRequested>) event).getInitiator().getUser().getUsername();
//      case ROOM_CREATED:
//        return ((RealTimeEvent<V4RoomCreated>) event).getSource().getStream().getStreamId();
//      case ROOM_UPDATED:
//        return ((RealTimeEvent<V4RoomUpdated>) event).getSource().getStream().getStreamId();
//      case USER_LEFT_ROOM:
//        return ((RealTimeEvent<V4UserLeftRoom>) event).getSource().getStream().getStreamId();
//      case ROOM_DEACTIVATED:
//        return ((RealTimeEvent<V4RoomDeactivated>) event).getSource().getStream().getStreamId();
//      case ROOM_REACTIVATED:
//        return ((RealTimeEvent<V4RoomReactivated>) event).getSource().getStream().getStreamId();
//      case USER_JOINED_ROOM:
//        return ((RealTimeEvent<V4UserJoinedRoom>) event).getSource().getStream().getStreamId();
//      case USER_REQUESTED_TO_JOIN_ROOM:
//        return ((RealTimeEvent<V4UserRequestedToJoinRoom>) event).getSource().getStream().getStreamId();
//      case ROOM_MEMBER_PROMOTED_TO_OWNER:
//        return ((RealTimeEvent<V4RoomMemberPromotedToOwner>) event).getSource().getStream().getStreamId();
//      case ROOM_MEMBER_DEMOTED_FROM_OWNER:
//        return ((RealTimeEvent<V4RoomMemberDemotedFromOwner>) event).getSource().getStream().getStreamId();
    }
    return null;
  }
}
