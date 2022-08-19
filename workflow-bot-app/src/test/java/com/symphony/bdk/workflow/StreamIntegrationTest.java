package com.symphony.bdk.workflow;

import static com.symphony.bdk.gen.api.model.StreamType.TypeEnum.POST;
import static com.symphony.bdk.gen.api.model.StreamType.TypeEnum.ROOM;
import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.gen.api.model.StreamFilter;
import com.symphony.bdk.gen.api.model.StreamType;
import com.symphony.bdk.gen.api.model.V2AdminStreamFilter;
import com.symphony.bdk.gen.api.model.V2AdminStreamList;
import com.symphony.bdk.gen.api.model.V2AdminStreamType;
import com.symphony.bdk.gen.api.model.V2MembershipList;
import com.symphony.bdk.gen.api.model.V2StreamAttributes;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.exception.SwadlNotValidException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("ConstantConditions") // for null pagination attribute with refEq
class StreamIntegrationTest extends IntegrationTest {

  @Test
  void getStream() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/stream/get-stream.swadl.yaml"));
    when(streamService.getStream("abc")).thenReturn(new V2StreamAttributes());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-stream"));

    verify(streamService, timeout(5000)).getStream("abc");
    assertThat(workflow).isExecuted();
  }

  @ParameterizedTest
  @CsvSource({"/stream/obo/get-stream-obo-valid-userid.swadl.yaml, /get-stream-obo-valid-userid",
      "/stream/obo/get-stream-obo-valid-username.swadl.yaml, /get-stream-obo-valid-username"})
  void getStreamObo(String workflowFile, String command) throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);
    when(oboStreamService.getStream("abc")).thenReturn(new V2StreamAttributes());

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    verify(oboStreamService, timeout(5000)).getStream("abc");
    assertThat(workflow).isExecuted();
  }

  @Test
  void getStreamOboUnauthorized() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/stream/obo/get-stream-obo-unauthorized.swadl.yaml"));

    when(bdkGateway.obo(any(String.class))).thenThrow(new RuntimeException("Unauthorized user"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-stream-obo-unauthorized"));

    assertThat(workflow).executed("getStreamOboUnauthorized")
        .notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  void getStreamMembers() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/stream/get-stream-members.swadl.yaml"));
    when(streamService.listStreamMembers("abc")).thenReturn(new V2MembershipList());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-stream-members"));

    verify(streamService, timeout(5000)).listStreamMembers("abc");
    assertThat(workflow).isExecuted();
  }

  @Test
  void getStreamMembersPagination() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/stream/get-stream-members-pagination.swadl.yaml"));
    when(streamService.listStreamMembers(eq("abc"), refEq(new PaginationAttribute(10, 10))))
        .thenReturn(new V2MembershipList());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-stream-members-pagination"));

    assertThat(workflow).isExecuted();
  }

  @Test
  void getStreams() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/stream/get-streams.swadl.yaml"));

    V2AdminStreamFilter filter = new V2AdminStreamFilter()
        .streamTypes(List.of(new V2AdminStreamType().type("IM"), new V2AdminStreamType().type("ROOM")))
        .status("ACTIVE")
        .origin("INTERNAL")
        .startDate(1629210917000L);
    when(streamService.listStreamsAdmin(refEq(filter))).thenReturn(new V2AdminStreamList());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-streams"));

    verify(streamService, timeout(5000)).listStreamsAdmin(refEq(filter));
    assertThat(workflow).isExecuted();
  }

  @Test
  void getStreamsPagination() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/stream/get-streams-pagination.swadl.yaml"));

    V2AdminStreamFilter filter = new V2AdminStreamFilter()
        .streamTypes(List.of(new V2AdminStreamType().type("IM"), new V2AdminStreamType().type("ROOM")))
        .status("ACTIVE")
        .origin("INTERNAL")
        .startDate(1629210917000L);
    when(streamService.listStreamsAdmin(refEq(filter), refEq(new PaginationAttribute(10, 10))))
        .thenReturn(new V2AdminStreamList());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-streams-pagination"));

    verify(streamService, timeout(5000)).listStreamsAdmin(refEq(filter), refEq(new PaginationAttribute(10, 10)));
    assertThat(workflow).isExecuted();
  }

  @Test
  void getUserStreams() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/stream/get-user-streams.swadl.yaml"));

    StreamFilter filter = new StreamFilter()
        .streamTypes(List.of(new StreamType().type(ROOM), new StreamType().type(POST)))
        .includeInactiveStreams(true);
    when(streamService.listStreams(refEq(filter))).thenReturn(Collections.emptyList());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-user-streams"));

    verify(streamService, timeout(5000)).listStreams(refEq(filter));
    assertThat(workflow).isExecuted();
  }

  @ParameterizedTest
  @CsvSource({"/stream/obo/get-user-streams-obo-valid-userid.swadl.yaml, /get-user-streams-obo-valid-userid",
      "/stream/obo/get-user-streams-obo-valid-username.swadl.yaml, /get-user-streams-obo-valid-username"})
  void getUserStreamsObo(String workflowFile, String command) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    StreamFilter filter = new StreamFilter()
        .streamTypes(List.of(new StreamType().type(ROOM), new StreamType().type(POST)))
        .includeInactiveStreams(true);

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);
    when(oboStreamService.listStreams(refEq(filter))).thenReturn(Collections.emptyList());

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    verify(oboStreamService, timeout(5000)).listStreams(refEq(filter));
    assertThat(workflow).isExecuted();
  }

  @Test
  void getUserStreamsOboUnauthorized() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/stream/obo/get-user-streams-obo-unauthorized.swadl.yaml"));

    when(bdkGateway.obo(any(String.class))).thenThrow(new RuntimeException("Unauthorized user"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-user-streams-obo-unauthorized"));

    assertThat(workflow).executed("getUserStreamsOboUnauthorized")
        .notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  void getUserStreamsPagination() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/stream/get-user-streams-pagination.swadl.yaml"));

    StreamFilter filter = new StreamFilter()
        .streamTypes(List.of(new StreamType().type(ROOM), new StreamType().type(POST)))
        .includeInactiveStreams(true);
    when(streamService.listStreams(refEq(filter), refEq(new PaginationAttribute(10, 10))))
        .thenReturn(Collections.emptyList());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-user-streams-pagination"));

    verify(streamService, timeout(5000)).listStreams(refEq(filter), refEq(new PaginationAttribute(10, 10)));
    assertThat(workflow).isExecuted();
  }

  @Test
  void getUserStreamsBadPagination() {
    assertThatThrownBy(
        () -> SwadlParser.fromYaml(
            getClass().getResourceAsStream("/stream/get-user-streams-bad-pagination.swadl.yaml")))
        .describedAs("Should fail at validation time because only skip property is set without limit")
        .isInstanceOf(SwadlNotValidException.class);
  }
}
