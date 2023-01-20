package com.symphony.bdk.workflow;

import com.symphony.bdk.core.OboServices;
import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.service.connection.ConnectionService;
import com.symphony.bdk.core.service.connection.OboConnectionService;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.message.OboMessageService;
import com.symphony.bdk.core.service.message.model.Attachment;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.core.service.stream.OboStreamService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.core.service.user.OboUserService;
import com.symphony.bdk.core.service.user.UserService;
import com.symphony.bdk.ext.group.SymphonyGroupService;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.UserConnection;
import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.gen.api.model.V4AttachmentInfo;
import com.symphony.bdk.gen.api.model.V4ConnectionAccepted;
import com.symphony.bdk.gen.api.model.V4Initiator;
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
import com.symphony.bdk.gen.api.model.V4Stream;
import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.gen.api.model.V4User;
import com.symphony.bdk.gen.api.model.V4UserJoinedRoom;
import com.symphony.bdk.gen.api.model.V4UserLeftRoom;
import com.symphony.bdk.gen.api.model.V4UserRequestedToJoinRoom;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.configuration.WorkflowBotConfiguration;
import com.symphony.bdk.workflow.engine.ResourceProvider;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.executor.BdkGateway;
import com.symphony.bdk.workflow.swadl.v1.Activity;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.restassured.RestAssured;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration
@Import(IntegrationTestConfiguration.class)
public abstract class IntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  WorkflowEngine engine;

  @Autowired
  ResourceProvider resourceProvider;

  @SuppressFBWarnings
  public static HistoryService historyService;

  @SuppressFBWarnings
  public static RuntimeService runtimeService;

  @SuppressFBWarnings
  public static RepositoryService repositoryService;

  // Mock WDK config
  @MockBean(name = "workflowBotConfiguration")
  WorkflowBotConfiguration workflowBotConfiguration;

  // Mock the BDK
  @MockBean
  AuthSession botSession;

  @MockBean
  OboServices oboServices;

  @MockBean(name = "oboMessageService")
  OboMessageService oboMessageService;

  @MockBean(name = "oboStreamService")
  OboStreamService oboStreamService;

  @MockBean(name = "oboUserService")
  OboUserService oboUserService;

  @MockBean(name = "oboConnectionService")
  OboConnectionService oboConnectionService;

  @MockBean(name = "streamService")
  StreamService streamService;

  @MockBean(name = "messageService")
  MessageService messageService;

  @MockBean(name = "userService")
  UserService userService;

  @MockBean(name = "connectionService")
  ConnectionService connectionService;

  @MockBean(name = "sessionService")
  SessionService sessionService;

  @MockBean(name = "groupService")
  SymphonyGroupService groupService;

  // BdkGateway is changed in setUpMocks method to return the beans above
  @MockBean(name = "springBdkGateway")
  BdkGateway bdkGateway;

  static {
    // we don't use nashorn, we don't care it is going to disappear
    System.setProperty("nashorn.args", "--no-deprecation-warning");
  }

  @Autowired
  public void setHistoryService(HistoryService historyService) {
    IntegrationTest.historyService = historyService;
  }

  @Autowired
  public void setRuntimeService(RuntimeService runtimeService) {
    IntegrationTest.runtimeService = runtimeService;
  }

  @Autowired
  public void setRepositoryService(RepositoryService repositoryService) {
    IntegrationTest.repositoryService = repositoryService;
  }

  protected static V4Message message(String msgId) {
    final V4Message message = new V4Message();
    message.setMessageId(msgId);
    return message;
  }

  protected static V4Message message(String msgId, String content) {
    return message(msgId).message(content);
  }

  protected static UserConnection connection(Long userId) {
    return connection(userId, null);
  }

  protected static UserConnection connection(Long userId, UserConnection.StatusEnum status) {
    final UserConnection userConnection = new UserConnection();
    userConnection.setUserId(userId);
    userConnection.setStatus(status);
    return userConnection;
  }

  protected static Stream stream(String streamId) {
    final Stream stream = new Stream();
    stream.setId(streamId);
    return stream;
  }

  @BeforeEach
  void setUpMocks() {
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn("MONITORING_TOKEN_VALUE");
    when(workflowBotConfiguration.getWorkflowsFolderPath()).thenReturn("");

    when(bdkGateway.messages()).thenReturn(this.messageService);
    when(bdkGateway.streams()).thenReturn(this.streamService);
    when(bdkGateway.connections()).thenReturn(this.connectionService);
    when(bdkGateway.users()).thenReturn(this.userService);
    when(bdkGateway.groups()).thenReturn(this.groupService);
    when(bdkGateway.obo(any(AuthSession.class))).thenReturn(this.oboServices);
    when(oboServices.messages()).thenReturn(this.oboMessageService);
    when(oboServices.streams()).thenReturn(this.oboStreamService);
    when(oboServices.users()).thenReturn(this.oboUserService);
    when(oboServices.connections()).thenReturn(this.oboConnectionService);
    when(sessionService.getSession()).thenReturn(new UserV2().displayName("bot"));

    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;

    when(bdkGateway.messages()).thenReturn(messageService);
  }

  // make sure we start the test with a clean engine to avoid the same /command to be registered
  @AfterEach
  void removeAllWorkflows() throws InterruptedException {
    for (int i = 0; i < 5; i++) {
      try {
        engine.undeployAll();
        return;
      } catch (Exception e) {
        // this might fail if processes are running at the same time, wait a bit a retry one more time
        Thread.sleep(100); // NOSONAR
      }
    }
  }

  public static RealTimeEvent<V4MessageSuppressed> messageSuppressed() {
    V4Initiator initiator = initiator();

    V4MessageSuppressed messageSuppressed = new V4MessageSuppressed();
    messageSuppressed.setMessageId("");
    messageSuppressed.setStream(new V4Stream());

    return new RealTimeEvent<>(initiator, messageSuppressed);
  }

  public static RealTimeEvent<V4InstantMessageCreated> imCreated() {
    V4Initiator initiator = initiator();

    V4InstantMessageCreated instantMessageCreated = new V4InstantMessageCreated();
    instantMessageCreated.setStream(new V4Stream());

    return new RealTimeEvent<>(initiator, instantMessageCreated);
  }

  public static RealTimeEvent<V4RoomCreated> roomCreated() {
    return new RealTimeEvent<>(initiator(), new V4RoomCreated());
  }

  public static RealTimeEvent<V4RoomUpdated> roomUpdated() {
    return new RealTimeEvent<>(initiator(), new V4RoomUpdated());
  }

  public static RealTimeEvent<V4RoomDeactivated> roomDeactivated() {
    return new RealTimeEvent<>(initiator(), new V4RoomDeactivated());
  }

  public static RealTimeEvent<V4RoomReactivated> roomReactivated() {
    return new RealTimeEvent<>(initiator(), new V4RoomReactivated());
  }

  public static RealTimeEvent<V4RoomMemberPromotedToOwner> roomMemberPromoted() {
    return new RealTimeEvent<>(initiator(), new V4RoomMemberPromotedToOwner());
  }

  public static RealTimeEvent<V4RoomMemberDemotedFromOwner> roomOwnerDemoted() {
    return new RealTimeEvent<>(initiator(), new V4RoomMemberDemotedFromOwner());
  }

  public static RealTimeEvent<V4ConnectionAccepted> connectionAccepted() {
    return new RealTimeEvent<>(initiator(), new V4ConnectionAccepted());
  }

  public static RealTimeEvent<V4UserRequestedToJoinRoom> connectionRequested() {
    return new RealTimeEvent<>(initiator(), new V4UserRequestedToJoinRoom());
  }

  public static RealTimeEvent<V4SharedPost> postShared() {
    return new RealTimeEvent<>(initiator(), new V4SharedPost());
  }

  public static RealTimeEvent<V4MessageSent> messageReceived(String streamId, String content, String messageId) {
    RealTimeEvent<V4MessageSent> event = messageReceived(streamId, content);
    event.getSource().getMessage().setMessageId(messageId);
    return event;
  }

  public static RealTimeEvent<V4MessageSent> messageReceived(String streamId, String content) {
    RealTimeEvent<V4MessageSent> event = messageReceived(content);
    V4Stream stream = new V4Stream();
    stream.setStreamId(streamId);
    event.getSource().getMessage().setStream(stream);
    return event;
  }

  public static RealTimeEvent<V4MessageSent> messageReceived(String content) {
    V4MessageSent messageSent = new V4MessageSent();
    V4Message message = new V4Message();
    message.setMessageId("msgId");
    message.setMessage("<presentationML>" + content + "</presentationML>");
    messageSent.setMessage(message);
    V4Stream stream = new V4Stream();
    stream.setStreamId("123");
    message.setStream(stream);

    return new RealTimeEvent<>(initiator(), messageSent);
  }

  private static V4Initiator initiator() {
    V4Initiator initiator = new V4Initiator();
    V4User user = new V4User();
    user.setUserId(123L);
    initiator.setUser(user);
    return initiator;
  }

  public static RealTimeEvent<V4UserJoinedRoom> userJoined() {
    V4User user = new V4User();
    user.setUserId(123L);
    V4Stream stream = new V4Stream();
    stream.setStreamId("123");
    V4UserJoinedRoom joinedRoom = new V4UserJoinedRoom();
    joinedRoom.affectedUser(user);
    joinedRoom.setStream(stream);

    return new RealTimeEvent<>(new V4Initiator(), joinedRoom);
  }

  public static RealTimeEvent<V4UserLeftRoom> userLeft() {
    V4User user = new V4User();
    user.setUserId(123L);
    V4Stream stream = new V4Stream();
    stream.setStreamId("123");
    V4UserLeftRoom leftRoom = new V4UserLeftRoom();
    leftRoom.affectedUser(user);
    leftRoom.setStream(stream);

    return new RealTimeEvent<>(new V4Initiator(), leftRoom);
  }

  public static RealTimeEvent<V4SymphonyElementsAction> form(String messageId,
      String formId, Map<String, Object> formReplies) {
    return form(messageId, formId, formReplies, "123");
  }

  public static RealTimeEvent<V4SymphonyElementsAction> form(String messageId, String formId,
                                                             Map<String, Object> formReplies, String streamId) {
    V4Initiator initiator = new V4Initiator();
    V4User user = new V4User();
    user.setUserId(123L);
    initiator.setUser(user);

    V4SymphonyElementsAction elementsAction = new V4SymphonyElementsAction();
    elementsAction.setFormMessageId(messageId);
    elementsAction.setFormId(formId);
    elementsAction.setFormValues(formReplies);
    elementsAction.setStream(new V4Stream().streamId(streamId));
    return new RealTimeEvent<>(initiator, elementsAction);
  }

  public static Boolean processIsCompleted(String processId) {
    List<HistoricProcessInstance> processes = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(processId).list();
    if (!processes.isEmpty()) {
      HistoricProcessInstance processInstance = processes.get(0);
      return processInstance.getState().equals("COMPLETED");
    }
    return false;
  }

  public static Map<String, Object> getVariable(String processId, String name) {
    HistoricVariableInstance var = historyService.createHistoricVariableInstanceQuery()
        .processInstanceId(processId)
        .variableName(name)
        .singleResult();
    if (var == null) {
      return Collections.emptyMap();
    } else {
      return new HashMap<>((Map<String, Object>) var.getValue());
    }
  }

  public static Optional<String> lastProcess() {
    List<HistoricProcessInstance> processes = historyService.createHistoricProcessInstanceQuery()
        .orderByProcessInstanceStartTime().desc()
        .list();
    if (processes.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(processes.get(0))
          .map(HistoricProcessInstance::getId);
    }
  }

  protected Optional<String> lastProcess(Workflow workflow) {
    List<HistoricProcessInstance> processes = historyService.createHistoricProcessInstanceQuery()
            .processDefinitionName(workflow.getId())
            .orderByProcessInstanceStartTime().desc()
            .list();
    if (processes.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(processes.get(0))
              .map(HistoricProcessInstance::getId);
    }
  }

  public static List<String> finishedProcessById(String workflowId) {
    return historyService.createHistoricProcessInstanceQuery()
            .processDefinitionKey(workflowId)
            .finished()
            .list()
            .stream()
            .map(HistoricProcessInstance::getId)
            .collect(Collectors.toList());
  }

  public static List<String> unfinishedProcessById(String workflowId) {
    return historyService.createHistoricProcessInstanceQuery()
            .processDefinitionKey(workflowId)
            .unfinished()
            .list()
            .stream()
            .map(HistoricProcessInstance::getId)
            .collect(Collectors.toList());
  }

  public static void assertExecuted(Workflow workflow) {
    String[] activityIds = workflow.getActivities().stream()
        .map(Activity::getActivity)
        .map(BaseActivity::getId)
        .toArray(String[]::new);
    assertExecuted(activityIds);
  }

  private static void assertExecuted(String... activityIds) {
    String process = lastProcess().orElseThrow();
    await().atMost(5, SECONDS).until(() -> processIsCompleted(process));

    List<HistoricActivityInstance> processes = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(process)
        .orderByHistoricActivityInstanceStartTime().asc()
        .orderByActivityName().asc()
        .list();

    assertThat(processes)
        .filteredOn(p -> !p.getActivityType().equals("signalStartEvent"))
        .filteredOn(p -> !p.getActivityType().equals("noneEndEvent"))
        .extracting(HistoricActivityInstance::getActivityName)
        .containsExactly(activityIds);
  }

  protected Message buildMessage(String content, List<Attachment> attachments) {
    return Message.builder().content(content).attachments(attachments).build();
  }

  protected static byte[] mockBase64ByteArray() {
    String randomString = UUID.randomUUID().toString();
    return Base64.getEncoder().encode(randomString.getBytes(StandardCharsets.UTF_8));
  }

  // This method makes a thread sleep to make a workflow times out
  protected static void sleepToTimeout(long durationInMilliSeconds) throws InterruptedException {
    Thread.sleep(durationInMilliSeconds);
  }

  protected V4Message createMessage(String msgId) {
    return createMessage(msgId, null, null);
  }

  protected V4Message createMessage(String msgId, String attachmentId, String attachmentName) {
    final V4Message actualMessage = new V4Message();
    actualMessage.setMessageId(msgId);

    final V4Stream v4Stream = new V4Stream();
    v4Stream.setStreamId("STREAM_ID");
    actualMessage.setStream(v4Stream);

    final List<V4AttachmentInfo> attachments =
        Collections.singletonList(new V4AttachmentInfo().id(attachmentId).name(attachmentName));
    actualMessage.setAttachments(attachments);

    return actualMessage;
  }

  protected String userMentionData(long userId) {
    return "{\n"
        + "  \"0\": {\n"
        + "    \"id\": [\n"
        + "      {\n"
        + "        \"type\": \"com.symphony.user.userId\",\n"
        + "        \"value\": \"" + userId + "\"\n"
        + "      }\n"
        + "    ],\n"
        + "    \"type\": \"com.symphony.user.mention\"\n"
        + "  }\n"
        + "}\n";
  }

}
