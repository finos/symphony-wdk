package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4ConnectionAccepted;
import com.symphony.bdk.gen.api.model.V4ConnectionRequested;
import com.symphony.bdk.gen.api.model.V4InstantMessageCreated;
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
import com.symphony.bdk.workflow.engine.camunda.WorkflowEventHandler;
import com.symphony.bdk.workflow.swadl.v1.EventResolver;
import com.symphony.bdk.workflow.swadl.v1.EventWithTimeout;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
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
import com.symphony.bdk.workflow.swadl.v1.event.UserJoinedRoomEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserLeftRoomEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserRequestedToJoinRoomEvent;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public abstract class AbstractActivityExecutor<T extends BaseActivity> {

  @Autowired
  private RuntimeService runtimeService;

  /*protected AbstractActivityExecutor(RuntimeService runtimeService) {
    this.runtimeService = runtimeService;
  }*/

  private List<String> retrieveKeysOfType(Map<String, Object> map, String type) {
    return map.keySet().stream().filter(key -> key.contains(type)).collect(Collectors.toList());
  }

  protected void setEventVariables(ActivityExecutorContext<T> execution) {
    EventWithTimeout onEvents = execution.getActivity().getOn();

    if (onEvents == null) {
      return;
    }

    EventResolver eventResolver = new EventResolver(onEvents);

    Map<String, Object> tempVariables = runtimeService.getVariables(execution.getProcessInstanceId())
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().startsWith(WorkflowEventHandler.TEMPORARY_EVENT_KEY))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    if (eventResolver.isEventOfType(MessageReceivedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(MessageReceivedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4MessageSent.class.getSimpleName());

      keys.forEach(key -> {
        V4MessageSent v4MessageSent = ((EventHolder<V4MessageSent>) execution.getVariables().get(key)).getSource();
        eventsOfType.stream()
            .filter(e -> {
              try {
                return StringUtils.isNotBlank(((MessageReceivedEvent) e).getId()) &&
                PresentationMLParser.getTextContent(v4MessageSent.getMessage().getMessage())
                    .equals(((MessageReceivedEvent) e).getContent());
              } catch (PresentationMLParserException ex) {
                return false;
              }
            })
            .findAny()
            .ifPresent(
                o -> runtimeService.setVariable(execution.getProcessInstanceId(), ((MessageReceivedEvent) o).getId(),
                    runtimeService.getVariable(execution.getProcessInstanceId(), key)));
      });
    }

    if (eventResolver.isEventOfType(FormRepliedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(FormRepliedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4SymphonyElementsAction.class.getSimpleName());

      keys.forEach(key -> {
        V4SymphonyElementsAction v4SymphonyElementsAction =
            ((EventHolder<V4SymphonyElementsAction>) execution.getVariables().get(key)).getSource();
        eventsOfType.stream()
            .filter(e -> StringUtils.isNotBlank(((MessageReceivedEvent) e).getId())
                && v4SymphonyElementsAction.getFormId() != null && v4SymphonyElementsAction.getFormId()
                .equals(((FormRepliedEvent) e).getFormId()))
            .findAny()
            .ifPresent(
                o -> runtimeService.setVariable(execution.getProcessInstanceId(), ((FormRepliedEvent) o).getId(),
                    runtimeService.getVariable(execution.getProcessInstanceId(), key)));
      });
    }

    if (eventResolver.isEventOfType(MessageSuppressedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(MessageSuppressedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4MessageSuppressed.class.getSimpleName());

      var ref = new Object() {
        V4MessageSuppressed v4MessageSuppressed = null;
      };

      if (!keys.isEmpty()) {
        ref.v4MessageSuppressed =
            ((EventHolder<V4MessageSuppressed>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((MessageSuppressedEvent) e).getId()))
          .forEach(
              e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((MessageSuppressedEvent) e).getId(),
                  ref.v4MessageSuppressed));
    }

    if (eventResolver.isEventOfType(PostSharedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(PostSharedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4SharedPost.class.getSimpleName());

      var ref = new Object() {
        V4SharedPost v4SharedPost = null;
      };

      if (!keys.isEmpty()) {
        ref.v4SharedPost =
            ((EventHolder<V4SharedPost>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((PostSharedEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((PostSharedEvent) e).getId(), ref.v4SharedPost));
    }

    if (eventResolver.isEventOfType(ImCreatedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(ImCreatedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4InstantMessageCreated.class.getSimpleName());

      var ref = new Object() {
        V4InstantMessageCreated v4InstantMessageCreated = null;
      };

      if (!keys.isEmpty()) {
        ref.v4InstantMessageCreated =
            ((EventHolder<V4InstantMessageCreated>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((ImCreatedEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((ImCreatedEvent) e).getId(), ref.v4InstantMessageCreated));
    }

    if (eventResolver.isEventOfType(RoomCreatedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(RoomCreatedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4RoomCreated.class.getSimpleName());

      var ref = new Object() {
        V4RoomCreated v4RoomCreated = null;
      };

      if (!keys.isEmpty()) {
        ref.v4RoomCreated =
            ((EventHolder<V4RoomCreated>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((RoomCreatedEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((RoomCreatedEvent) e).getId(), ref.v4RoomCreated));
    }

    if (eventResolver.isEventOfType(RoomUpdatedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(RoomCreatedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4RoomUpdated.class.getSimpleName());

      var ref = new Object() {
        V4RoomUpdated v4RoomUpdated = null;
      };

      if (!keys.isEmpty()) {
        ref.v4RoomUpdated =
            ((EventHolder<V4RoomUpdated>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }


      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((RoomUpdatedEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((RoomUpdatedEvent) e).getId(), ref.v4RoomUpdated));
    }

    if (eventResolver.isEventOfType(RoomDeactivatedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(RoomDeactivatedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4RoomDeactivated.class.getSimpleName());

      var ref = new Object() {
        V4RoomDeactivated v4RoomDeactivated = null;
      };

      if (!keys.isEmpty()) {
        ref.v4RoomDeactivated =
            ((EventHolder<V4RoomDeactivated>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((RoomDeactivatedEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((RoomDeactivatedEvent) e).getId(), ref.v4RoomDeactivated));
    }

    if (eventResolver.isEventOfType(RoomReactivatedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(RoomReactivatedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4RoomReactivated.class.getSimpleName());

      var ref = new Object() {
        V4RoomReactivated v4RoomReactivated = null;
      };

      if (!keys.isEmpty()) {
        ref.v4RoomReactivated =
            ((EventHolder<V4RoomReactivated>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((RoomReactivatedEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((RoomReactivatedEvent) e).getId(), ref.v4RoomReactivated));
    }

    if (eventResolver.isEventOfType(RoomMemberPromotedToOwnerEvent.class.getSimpleName())) {
      List<Object> eventsOfType =
          eventResolver.getAllEventsOfType(RoomMemberPromotedToOwnerEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4RoomMemberPromotedToOwner.class.getSimpleName());

      var ref = new Object() {
        V4RoomMemberPromotedToOwner v4RoomMemberPromotedToOwner = null;
      };

      if (!keys.isEmpty()) {
        ref.v4RoomMemberPromotedToOwner =
            ((EventHolder<V4RoomMemberPromotedToOwner>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((RoomMemberPromotedToOwnerEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((RoomMemberPromotedToOwnerEvent) e).getId(), ref.v4RoomMemberPromotedToOwner));
    }

    if (eventResolver.isEventOfType(RoomMemberDemotedFromOwnerEvent.class.getSimpleName())) {
      List<Object> eventsOfType =
          eventResolver.getAllEventsOfType(RoomMemberDemotedFromOwnerEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4RoomMemberDemotedFromOwner.class.getSimpleName());

      var ref = new Object() {
        V4RoomMemberDemotedFromOwner v4RoomMemberDemotedFromOwner = null;
      };

      if (!keys.isEmpty()) {
        ref.v4RoomMemberDemotedFromOwner =
            ((EventHolder<V4RoomMemberDemotedFromOwner>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((RoomMemberDemotedFromOwnerEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((RoomMemberDemotedFromOwnerEvent) e).getId(), ref.v4RoomMemberDemotedFromOwner));
    }

    if (eventResolver.isEventOfType(UserJoinedRoomEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(UserJoinedRoomEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4UserJoinedRoom.class.getSimpleName());

      var ref = new Object() {
        V4UserJoinedRoom v4UserJoinedRoom = null;
      };

      if (!keys.isEmpty()) {
        ref.v4UserJoinedRoom =
            ((EventHolder<V4UserJoinedRoom>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((UserJoinedRoomEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((UserJoinedRoomEvent) e).getId(), ref.v4UserJoinedRoom));
    }

    if (eventResolver.isEventOfType(UserLeftRoomEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(UserLeftRoomEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4UserLeftRoom.class.getSimpleName());

      var ref = new Object() {
        V4UserLeftRoom v4UserLeftRoom = null;
      };

      if (!keys.isEmpty()) {
        ref.v4UserLeftRoom =
            ((EventHolder<V4UserLeftRoom>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((UserLeftRoomEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((UserLeftRoomEvent) e).getId(), ref.v4UserLeftRoom));
    }

    if (eventResolver.isEventOfType(UserRequestedToJoinRoomEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(UserRequestedToJoinRoomEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4UserRequestedToJoinRoom.class.getSimpleName());

      var ref = new Object() {
        V4UserRequestedToJoinRoom v4UserRequestedToJoinRoom = null;
      };

      if (!keys.isEmpty()) {
        ref.v4UserRequestedToJoinRoom =
            ((EventHolder<V4UserRequestedToJoinRoom>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((UserRequestedToJoinRoomEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((UserRequestedToJoinRoomEvent) e).getId(), ref.v4UserRequestedToJoinRoom));
    }

    if (eventResolver.isEventOfType(ConnectionRequestedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(ConnectionRequestedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4ConnectionRequested.class.getSimpleName());

      var ref = new Object() {
        V4ConnectionRequested v4ConnectionRequested = null;
      };

      if (!keys.isEmpty()) {
        ref.v4ConnectionRequested =
            ((EventHolder<V4ConnectionRequested>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((ConnectionRequestedEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((ConnectionRequestedEvent) e).getId(), ref.v4ConnectionRequested));
    }

    if (eventResolver.isEventOfType(ConnectionAcceptedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(ConnectionAcceptedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4ConnectionAccepted.class.getSimpleName());

      var ref = new Object() {
        V4ConnectionAccepted v4ConnectionAccepted = null;
      };

      if (!keys.isEmpty()) {
        ref.v4ConnectionAccepted =
            ((EventHolder<V4ConnectionAccepted>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((ConnectionAcceptedEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((ConnectionAcceptedEvent) e).getId(), ref.v4ConnectionAccepted));
    }

    if (eventResolver.isEventOfType(RequestReceivedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(RequestReceivedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, RequestReceivedEvent.class.getSimpleName());

      var ref = new Object() {
        RequestReceivedEvent requestReceivedEvent = null;
      };

      if (!keys.isEmpty()) {
        ref.requestReceivedEvent =
            ((EventHolder<RequestReceivedEvent>) runtimeService.getVariable(execution.getProcessInstanceId(),
                keys.get(0))).getSource();
      }

      eventsOfType.stream()
          .filter(e -> StringUtils.isNotBlank(((RequestReceivedEvent) e).getId()))
          .forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(), ((RequestReceivedEvent) e).getId(),
              ref.requestReceivedEvent));
    }

    // remove temporary variables
    runtimeService.removeVariables(execution.getProcessInstanceId(), tempVariables.keySet());
  }

}
