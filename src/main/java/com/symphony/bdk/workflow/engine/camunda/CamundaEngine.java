package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.gen.api.model.V4ConnectionAccepted;
import com.symphony.bdk.gen.api.model.V4ConnectionRequested;
import com.symphony.bdk.gen.api.model.V4InstantMessageCreated;
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
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.validator.YamlValidator;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CamundaEngine implements WorkflowEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaEngine.class);
  private static final String STREAM_ID = "streamId";
  private static final String MESSAGE_PREFIX = "message_";
  private static final String PROCESS_ID_PREFIX = "processId_";
  private static final String FORM_REPLY_PREFIX = "formReply_";

  private final String UID = "uid";
  private final String UIDS = "uids";

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private RepositoryService repositoryService;

  @Autowired
  private CamundaBpmnBuilder bpmnBuilder;

  @Override
  public void execute(Workflow workflow) throws IOException {
    LOGGER.info("Executing workflow {}", workflow.getName());
    bpmnBuilder.addWorkflow(workflow);
  }

  @Override
  public void stopAll() {
    for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Override
  public void messageReceived(String streamId, String content) {
    if (!content.startsWith(YamlValidator.YAML_VALIDATION_COMMAND)) {
      // content being the command to start a workflow
      runtimeService.startProcessInstanceByMessage(MESSAGE_PREFIX + content,
          buildMapFor(STREAM_ID, streamId));
    }
  }

  @Override
  public void messageSuppressed(RealTimeEvent<V4MessageSuppressed> event) {
    String streamId = event.getSource().getStream().getStreamId();
    String messageId = event.getSource().getMessageId();
    LOGGER.info("Message suppressed from {}", streamId);
    runtimeService.startProcessInstanceByMessage(MESSAGE_PREFIX + messageId,
        buildMapFor(STREAM_ID, streamId));
  }

  @Override
  public void formReceived(String messageId, String formId, Map<String, Object> formReplies) {
    // we expect the activity id to be the same as the form id to work
    // correlation across processes is based on the message id tha was created to send the form
    runtimeService.createMessageCorrelation(FORM_REPLY_PREFIX + formId)
        .processInstanceVariableEquals(formId + ".msgId", messageId)
        .setVariables(Collections.singletonMap(formId, formReplies))
        .correlate();
  }

  @Override
  public void postReceived(RealTimeEvent<V4SharedPost> event) {
    String streamId = event.getSource().getMessage().getStream().getStreamId();
    String content = event.getSource().getMessage().getMessage();
    LOGGER.info("Post received {}", streamId);
    runtimeService.startProcessInstanceByMessage(MESSAGE_PREFIX + content,
        buildMapFor(STREAM_ID, streamId));
  }

  @Override
  public void IMReceived(RealTimeEvent<V4InstantMessageCreated> event) {
    String streamId = event.getSource().getStream().getStreamId();
    LOGGER.info("IM received {}", streamId);
    runtimeService.startProcessInstanceById(PROCESS_ID_PREFIX + streamId,
        buildMapFor(STREAM_ID, streamId));

  }

  @Override
  public void roomCreated(RealTimeEvent<V4RoomCreated> event) {
    String streamId = event.getSource().getStream().getStreamId();
    LOGGER.info("Room created {}", streamId);
    runtimeService.startProcessInstanceById(PROCESS_ID_PREFIX + streamId,
        buildMapFor(STREAM_ID, streamId));
  }

  @Override
  public void roomUpdated(RealTimeEvent<V4RoomUpdated> event) {
    String streamId = event.getSource().getStream().getStreamId();
    LOGGER.info("Room updated {}", streamId);
    runtimeService.startProcessInstanceById(PROCESS_ID_PREFIX + streamId,
        buildMapFor(STREAM_ID, streamId));
  }

  @Override
  public void roomDeactivated(RealTimeEvent<V4RoomDeactivated> event) {
    String streamId = event.getSource().getStream().getStreamId();
    LOGGER.info("Room deactivated {}", streamId);
    runtimeService.startProcessInstanceById(PROCESS_ID_PREFIX + streamId,
        buildMapFor(STREAM_ID, streamId));
  }

  @Override
  public void roomReactivated(RealTimeEvent<V4RoomReactivated> event) {
    String streamId = event.getSource().getStream().getStreamId();
    LOGGER.info("Room reactivated {}", streamId);
    runtimeService.startProcessInstanceById(PROCESS_ID_PREFIX + streamId,
        buildMapFor(STREAM_ID, streamId));
  }

  @Override
  public void userRequestedToJoinRoom(RealTimeEvent<V4UserRequestedToJoinRoom> event) {
    String streamId = event.getSource().getStream().getStreamId();
    List<Long> uids = event.getSource().getAffectedUsers().stream()
        .map(u -> u.getUserId()).collect(Collectors.toList());
    LOGGER.info("Users [{}] requested to join room {}", uids, streamId);

    Map<String, Object> processVariables = buildMapFor(STREAM_ID, streamId);
    buildMapFor(processVariables, UIDS, uids);

    runtimeService.startProcessInstanceById(PROCESS_ID_PREFIX + streamId,
        processVariables);
  }

  @Override
  public void userJoinedRoom(RealTimeEvent<V4UserJoinedRoom> event) {
    String streamId = event.getSource().getStream().getStreamId();
    Long uid = event.getSource().getAffectedUser().getUserId();
    LOGGER.info("User {} joined room {}", uid, streamId);

    Map<String, Object> processVariables = buildMapFor(STREAM_ID, streamId);
    buildMapFor(processVariables, UID, uid);

    runtimeService.startProcessInstanceById(PROCESS_ID_PREFIX + streamId,
        processVariables);
  }

  @Override
  public void userLeftRoom(RealTimeEvent<V4UserLeftRoom> event) {
    String streamId = event.getSource().getStream().getStreamId();
    Long uid = event.getSource().getAffectedUser().getUserId();
    LOGGER.info("User {} left room {}", uid, streamId);

    Map<String, Object> processVariables = buildMapFor(STREAM_ID, streamId);
    buildMapFor(processVariables, UID, uid);

    runtimeService.startProcessInstanceById(PROCESS_ID_PREFIX + streamId,
        processVariables);
  }

  @Override
  public void roomMemberPromotedToOwner(RealTimeEvent<V4RoomMemberPromotedToOwner> event) {
    String streamId = event.getSource().getStream().getStreamId();
    Long uid = event.getSource().getAffectedUser().getUserId();
    LOGGER.info("User {} promoted to owner of room {}", uid, streamId);

    Map<String, Object> processVariables = buildMapFor(STREAM_ID, streamId);
    buildMapFor(processVariables, UID, uid);

    runtimeService.startProcessInstanceById(PROCESS_ID_PREFIX + streamId,
        processVariables);
  }

  @Override
  public void roomMemberDemotedFromOwner(RealTimeEvent<V4RoomMemberDemotedFromOwner> event) {
    String streamId = event.getSource().getStream().getStreamId();
    Long uid = event.getSource().getAffectedUser().getUserId();
    LOGGER.info("User {} demoted from owner of room {}", uid, streamId);

    Map<String, Object> processVariables = buildMapFor(STREAM_ID, streamId);
    buildMapFor(processVariables, UID, uid);

    runtimeService.startProcessInstanceById(PROCESS_ID_PREFIX + streamId,
        processVariables);
  }

  @Override
  public void connectionRequested(RealTimeEvent<V4ConnectionRequested> event) {
    Long userSentRequest = event.getInitiator().getUser().getUserId();
    Long userReceivedRequest = event.getSource().getToUser().getUserId();

    LOGGER.info("User {} requested connection to {}", userSentRequest, userReceivedRequest);

  }

  @Override
  public void connectionAccepted(RealTimeEvent<V4ConnectionAccepted> event) {
    Long userAcceptRequest = event.getInitiator().getUser().getUserId();
    Long userSentRequest = event.getSource().getFromUser().getUserId();

    LOGGER.info("User {} accepted connection to {}", userAcceptRequest, userSentRequest);
  }

  private Map<String, Object> buildMapFor (String key, Object value) {
    return buildMapFor(null, key, value);
  }

  private Map<String, Object> buildMapFor(Map<String, Object> variables, String key, Object value) {
    if (variables == null) {
      variables = new HashMap<>();
    }

    variables.put(key, value);

    return variables;
  }
}
