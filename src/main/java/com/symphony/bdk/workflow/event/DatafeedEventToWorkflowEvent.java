package com.symphony.bdk.workflow.event;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4ConnectionAccepted;
import com.symphony.bdk.gen.api.model.V4ConnectionRequested;
import com.symphony.bdk.gen.api.model.V4InstantMessageCreated;
import com.symphony.bdk.gen.api.model.V4Message;
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
import com.symphony.bdk.workflow.lang.validator.YamlValidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DatafeedEventToWorkflowEvent {

  private static final String FORM_REPLY = "formReply";

  @Autowired
  private WorkflowEngine workflowEngine;

  @EventListener
  public void onMessageSent(RealTimeEvent<V4MessageSent> event) throws PresentationMLParserException {
    if (!PresentationMLParser.getTextContent(event.getSource().getMessage().getMessage())
        .equals(YamlValidator.YAML_VALIDATION_COMMAND)) {
      String streamId = event.getSource().getMessage().getStream().getStreamId();
      String content = PresentationMLParser.getTextContent(event.getSource().getMessage().getMessage());
      workflowEngine.messageReceived(streamId, content);
    }
  }

  @EventListener
  public void onSymphonyElementsAction(RealTimeEvent<V4SymphonyElementsAction> event) {
    Map<String, Object> formReplies = (Map<String, Object>) event.getSource().getFormValues();
    String formId = event.getSource().getFormId();
    workflowEngine.formReceived(event.getSource().getFormMessageId(), formId, formReplies);
  }

  @EventListener
  public void onMessage(RealTimeEvent<V4Message> event) throws PresentationMLParserException {
    String streamId = event.getSource().getStream().getStreamId();
    String content = PresentationMLParser.getTextContent(event.getSource().getMessage());
    workflowEngine.messageSent(streamId, content);
  }

  @EventListener
  public void onSharedPost(RealTimeEvent<V4SharedPost> event) {
  }

  @EventListener
  public void onInstantMessageCreated(RealTimeEvent<V4InstantMessageCreated> event) {}

  @EventListener
  public void onRoomCreated(RealTimeEvent<V4RoomCreated> event) {}

  @EventListener
  public void onRoomUpdated(RealTimeEvent<V4RoomUpdated> event) {}

  @EventListener
  public void onRoomDeactivated(RealTimeEvent<V4RoomDeactivated> event) {}

  @EventListener
  public void onRoomReactivated(RealTimeEvent<V4RoomReactivated> event) {}

  @EventListener
  public void onUserRequestedToJoinRoom(RealTimeEvent<V4UserRequestedToJoinRoom> event) {}

  @EventListener
  public void onUserJoinedRoom(RealTimeEvent<V4UserJoinedRoom> event) {}

  @EventListener
  public void onUserLeftRoom(RealTimeEvent<V4UserLeftRoom> event) {}

  @EventListener
  public void onRoomMemberPromotedToOwner(RealTimeEvent<V4RoomMemberPromotedToOwner> event) {}

  @EventListener
  public void onRoomMemberDemotedFromOwner(RealTimeEvent<V4RoomMemberDemotedFromOwner> event) {}

  @EventListener
  public void onConnectionRequested(RealTimeEvent<V4ConnectionRequested> event) {}

  @EventListener
  public void onConnectionAccepted(RealTimeEvent<V4ConnectionAccepted> event) {}

  @EventListener
  public void onMessageSuppressed(RealTimeEvent<V4MessageSuppressed> event) {}
}
