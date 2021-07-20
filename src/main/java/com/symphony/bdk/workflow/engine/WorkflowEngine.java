package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.gen.api.model.V4ConnectionAccepted;
import com.symphony.bdk.gen.api.model.V4ConnectionRequested;
import com.symphony.bdk.gen.api.model.V4InstantMessageCreated;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageSuppressed;
import com.symphony.bdk.gen.api.model.V4RoomCreated;
import com.symphony.bdk.gen.api.model.V4RoomDeactivated;
import com.symphony.bdk.gen.api.model.V4RoomMemberDemotedFromOwner;
import com.symphony.bdk.gen.api.model.V4RoomMemberPromotedToOwner;
import com.symphony.bdk.gen.api.model.V4RoomReactivated;
import com.symphony.bdk.gen.api.model.V4RoomUpdated;
import com.symphony.bdk.gen.api.model.V4SharedPost;
import com.symphony.bdk.gen.api.model.V4UserJoinedRoom;
import com.symphony.bdk.gen.api.model.V4UserLeftRoom;
import com.symphony.bdk.gen.api.model.V4UserRequestedToJoinRoom;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import java.io.IOException;
import java.util.Map;

public interface WorkflowEngine {

  void execute(Workflow workflow) throws IOException;

  void messageReceived(String streamId, String content);

  /**
   * @param messageId The form's message id (created when the form is submitted)
   */
  void formReceived(String messageId, String formId, Map<String, Object> formReplies);

  void stopAll();

  void postReceived(RealTimeEvent<V4SharedPost> event);

  void IMReceived(RealTimeEvent<V4InstantMessageCreated> event);

  void roomCreated(RealTimeEvent<V4RoomCreated> event);

  void roomUpdated(RealTimeEvent<V4RoomUpdated> event);

  void roomDeactivated(RealTimeEvent<V4RoomDeactivated> event);

  void roomReactivated(RealTimeEvent<V4RoomReactivated> event);

  void userRequestedToJoinRoom(RealTimeEvent<V4UserRequestedToJoinRoom> event);

  void userJoinedRoom(RealTimeEvent<V4UserJoinedRoom> event);

  void userLeftRoom(RealTimeEvent<V4UserLeftRoom> event);

  void roomMemberPromotedToOwner(RealTimeEvent<V4RoomMemberPromotedToOwner> event);

  void roomMemberDemotedFromOwner(RealTimeEvent<V4RoomMemberDemotedFromOwner> event);

  void connectionRequested(RealTimeEvent<V4ConnectionRequested> event);

  void connectionAccepted(RealTimeEvent<V4ConnectionAccepted> event);

  void messageSuppressed(RealTimeEvent<V4MessageSuppressed> event);
}
