package com.symphony.bdk.workflow;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.message.model.Attachment;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.core.service.user.UserService;
import com.symphony.bdk.gen.api.model.V4Initiator;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.gen.api.model.V4Stream;
import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.gen.api.model.V4User;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.ResourceProvider;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.swadl.v1.Activity;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
@Import(IntegrationTestConfiguration.class)
abstract class IntegrationTest {

  @Autowired
  WorkflowEngine engine;

  @Autowired
  ResourceProvider resourceProvider;

  @Autowired
  HistoryService historyService;

  // Mock the BDK
  @MockBean
  AuthSession botSession;

  @MockBean(name = "streamService")
  StreamService streamService;

  @MockBean(name = "messageService")
  MessageService messageService;

  @MockBean(name = "userService")
  UserService userService;

  @MockBean(name = "sessionService")
  SessionService sessionService;

  static {
    // we don't use nashorn, we don't care it is going to disappear
    System.setProperty("nashorn.args", "--no-deprecation-warning");
  }

  protected static V4Message message(String msgId) {
    final V4Message message = new V4Message();
    message.setMessageId(msgId);
    return message;
  }

  @BeforeEach
  void setUpMocks() {
    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("msgId"));
  }

  // make sure we start the test with a clean engine to avoid the same /command to be registered
  @AfterEach
  void removeAllWorkflows() throws InterruptedException {
    for (int i = 0; i < 5; i++) {
      try {
        engine.stopAll();
        return;
      } catch (Exception e) {
        // this might fail if processes are running at the same time, wait a bit a retry one more time
        Thread.sleep(100); // NOSONAR
      }
    }
  }

  public static RealTimeEvent<V4MessageSent> messageReceived(String streamId, String content) {
    RealTimeEvent<V4MessageSent> event = messageReceived(content);
    V4Stream stream = new V4Stream();
    stream.setStreamId(streamId);
    event.getSource().getMessage().setStream(stream);
    return event;
  }

  public static RealTimeEvent<V4MessageSent> messageReceived(String content) {
    V4Initiator initiator = new V4Initiator();
    V4User user = new V4User();
    user.setUserId(123L);
    initiator.setUser(user);

    V4MessageSent messageSent = new V4MessageSent();
    V4Message message = new V4Message();
    message.setMessage("<presentationML>" + content + "</presentationML>");
    messageSent.setMessage(message);

    V4Stream stream = new V4Stream();
    stream.setStreamId("123");
    message.setStream(stream);

    return new RealTimeEvent<>(initiator, messageSent);
  }

  public static RealTimeEvent<V4SymphonyElementsAction> form(String messageId,
      String formId, Map<String, Object> formReplies) {
    V4Initiator initiator = new V4Initiator();
    V4User user = new V4User();
    user.setUserId(123L);
    initiator.setUser(user);

    V4SymphonyElementsAction elementsAction = new V4SymphonyElementsAction();
    elementsAction.setFormMessageId(messageId);
    elementsAction.setFormId(formId);
    elementsAction.setFormValues(formReplies);
    return new RealTimeEvent<>(initiator, elementsAction);
  }

  protected Boolean processIsCompleted(String processId) {
    List<HistoricProcessInstance> processes = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(processId).list();
    if (!processes.isEmpty()) {
      HistoricProcessInstance processInstance = processes.get(0);
      return processInstance.getState().equals("COMPLETED");
    }
    return false;
  }

  protected Optional<String> lastProcess() {
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
        .processDefinitionName(workflow.getName())
        .orderByProcessInstanceStartTime().desc()
        .list();
    if (processes.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.ofNullable(processes.get(0))
          .map(HistoricProcessInstance::getId);
    }
  }

  protected void assertExecuted(Workflow workflow) {
    String[] activityIds = workflow.getActivities().stream()
        .map(Activity::getActivity)
        .map(BaseActivity::getId)
        .toArray(String[]::new);
    assertExecuted(activityIds);
  }

  private void assertExecuted(String... activityIds) {
    String process = lastProcess().orElseThrow();
    await().atMost(5, SECONDS).until(() -> processIsCompleted(process));

    List<HistoricActivityInstance> processes = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(process)
        .orderByHistoricActivityInstanceStartTime().asc()
        .orderByActivityName().asc()
        .list();

    assertThat(processes)
        .filteredOn(p -> !p.getActivityType().equals("signalStartEvent"))
        .extracting(HistoricActivityInstance::getActivityName)
        .containsExactly(activityIds);
  }

  protected Message buildMessage(String content, List<Attachment> attachments) {
    return Message.builder().content(content).attachments(attachments).build();
  }

  protected void assertMessage(Message actual, Message expected) throws IOException {
    assertThat(actual.getContent()).as("The same content should be found").isEqualTo(expected.getContent());
    assertThat(actual.getData()).as("The same data should be found").isEqualTo(expected.getData());
    this.assertAttachments(actual.getAttachments(), expected.getAttachments());
  }

  private void assertAttachments(List<Attachment> actual, List<Attachment> expected) throws IOException {
    if (actual == null || actual.isEmpty() || expected == null || expected.isEmpty()) {
      assertThat(actual).isEqualTo(expected);
    } else {
      assertThat(actual.size()).isEqualTo(expected.size());
      for (Attachment actualAttachment : actual) {
        boolean isFound = false;
        for (Attachment expectedAttachment : expected) {

          // reset both InputStreams to the initial state,
          // otherwise the equality check will return false even if they have the same values in the byte array
          actualAttachment.getContent().reset();
          expectedAttachment.getContent().reset();

          if (IOUtils.contentEquals(expectedAttachment.getContent(), actualAttachment.getContent())
              && expectedAttachment.getFilename().equals(actualAttachment.getFilename())) {
            isFound = true;
            break;
          }
        }
        assertThat(isFound).isTrue();
      }
    }
  }

  protected static Message content(final String content) {
    return argThat(new ArgumentMatcher<>() {
      @Override
      public boolean matches(Message argument) {
        return argument.getContent().equals("<messageML>" + content + "</messageML>");
      }

      @Override
      public String toString() {
        return "messageWithContent(" + content + ")";
      }
    });
  }

  protected static Message contains(final String content) {
    return argThat(new ArgumentMatcher<>() {
      @Override
      public boolean matches(Message argument) {
        return argument.getContent().contains(content);
      }

      @Override
      public String toString() {
        return "messageContainingContent(" + content + ")";
      }
    });
  }


}
