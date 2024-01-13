package com.symphony.bdk.workflow;

import com.symphony.bdk.core.service.connection.constant.ConnectionStatus;
import com.symphony.bdk.gen.api.model.UserConnection;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionsIntegrationTest extends IntegrationTest {

  private static final String OUTPUT_CONNECTION_KEY = "%s.outputs.connection";
  private static final String OUTPUTS_CONNECTIONS_KEY = "%s.outputs.connections";

  @Test
  @DisplayName(
      "Given a user and a connection status, when the workflow is triggered,"
          + "then all connections having this status are returned")
  void listConnections() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/connection/get-connections.swadl.yaml"));

    final List<Long> userIds = Arrays.asList(666L, 777L);
    final List<UserConnection> userConnections = Arrays.asList(connection(666L, UserConnection.StatusEnum.ACCEPTED),
        connection(777L, UserConnection.StatusEnum.ACCEPTED));

    when(connectionService.listConnections(ConnectionStatus.ACCEPTED, userIds)).thenReturn(userConnections);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-connections"));

    verify(connectionService, timeout(5000)).listConnections(ConnectionStatus.ACCEPTED, userIds);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_CONNECTIONS_KEY, "getConnections"), userConnections);
  }

  @Test
  void listConnectionsNoParam() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/connection/get-connections-no-params.swadl.yaml"));

    final List<UserConnection> userConnections = Arrays.asList(connection(666L, UserConnection.StatusEnum.ACCEPTED),
        connection(777L, UserConnection.StatusEnum.ACCEPTED));

    when(connectionService.listConnections(null, null)).thenReturn(userConnections);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-connections-no-params"));

    verify(connectionService, timeout(5000)).listConnections(null, null);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_CONNECTIONS_KEY, "getConnections"), userConnections);
  }

  @ParameterizedTest
  @CsvSource(
      {"/connection/obo/get-connections-obo-valid-username.swadl.yaml, /get-connections-obo-valid-username, "
          + "getConnectionsOboValidUsername",
          "/connection/obo/get-connections-obo-valid-userid.swadl.yaml, /get-connections-obo-valid-userid, "
              + "getConnectionsOboValidUserid"})
  void listConnectionsStatusObo(String workflowFile, String command, String output) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    final List<Long> userIds = Arrays.asList(666L, 777L);
    final List<UserConnection> userConnections = Arrays.asList(connection(666L, UserConnection.StatusEnum.ACCEPTED),
        connection(777L, UserConnection.StatusEnum.ACCEPTED));

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);
    when(oboConnectionService.listConnections(ConnectionStatus.ACCEPTED, userIds)).thenReturn(userConnections);

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    verify(oboConnectionService, timeout(5000)).listConnections(ConnectionStatus.ACCEPTED, userIds);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_CONNECTIONS_KEY, output), userConnections);
  }

  @Test
  void listConnectionsStatusOboUnauthorized() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/connection/obo/get-connections-obo-unauthorized.swadl.yaml"));

    when(bdkGateway.obo(any(Long.class))).thenThrow(new RuntimeException("Unauthorized user"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-connections-obo-unauthorized"));

    assertThat(workflow).executed("getConnectionsOboUnauthorized").notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  @DisplayName("Given a connection with a user, when the workflow is triggered, then the connection is returned")
  void getConnectionStatus() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/connection/get-connection.swadl.yaml"));

    final Long userId = 1234L;
    final UserConnection userConnection = connection(userId);

    when(connectionService.getConnection(userId)).thenReturn(userConnection);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-connection"));

    verify(connectionService, timeout(5000)).getConnection(userId);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_CONNECTION_KEY, "getConnection"), userConnection);
  }

  @ParameterizedTest
  @CsvSource(
      {"/connection/obo/get-connection-obo-valid-username.swadl.yaml, /get-connection-obo-valid-username, "
          + "getConnectionOboValidUsername",
          "/connection/obo/get-connection-obo-valid-userid.swadl.yaml, /get-connection-obo-valid-userid, "
              + "getConnectionOboValidUserid"})
  void getConnectionStatusObo(String workflowFile, String command, String output) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    final Long userId = 1234L;
    final UserConnection userConnection = connection(userId);

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);
    when(oboConnectionService.getConnection(userId)).thenReturn(userConnection);

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    verify(oboConnectionService, timeout(5000)).getConnection(userId);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_CONNECTION_KEY, output), userConnection);
  }

  @Test
  void getConnectionStatusOboUnauthorized() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/connection/obo/get-connection-obo-unauthorized.swadl.yaml"));

    when(bdkGateway.obo(any(Long.class))).thenThrow(new RuntimeException("Unauthorized user"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-connection-obo-unauthorized"));

    assertThat(workflow).executed("getConnectionOboUnauthorized").notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  @DisplayName(
      "Given a user, when the workflow is triggered, then a connection to him is created")
  void createConnectionStatus() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/connection/create-connection.swadl.yaml"));

    final Long userId = 1234L;
    final UserConnection userConnection = connection(userId, UserConnection.StatusEnum.PENDING_OUTGOING);

    when(connectionService.createConnection(userId)).thenReturn(userConnection);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/create-connection"));

    verify(connectionService, timeout(5000)).createConnection(userId);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_CONNECTION_KEY, "createConnection"), userConnection);
  }

  @ParameterizedTest
  @CsvSource(
      {"/connection/obo/create-connection-obo-valid-username.swadl.yaml, /create-connection-obo-valid-username, "
          + "createConnectionOboValidUsername",
          "/connection/obo/create-connection-obo-valid-userid.swadl.yaml, /create-connection-obo-valid-userid, "
              + "createConnectionOboValidUserid"})
  void createConnectionObo(String workflowFile, String command, String outputName) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    final Long userId = 1234L;
    final UserConnection userConnection = connection(userId, UserConnection.StatusEnum.PENDING_OUTGOING);

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);
    when(oboConnectionService.createConnection(userId)).thenReturn(userConnection);

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    verify(oboConnectionService, timeout(5000)).createConnection(userId);

    assertThat(workflow).isExecuted().hasOutput(String.format(OUTPUT_CONNECTION_KEY, outputName), userConnection);
  }

  @Test
  void createConnectionOboUnauthorized() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(
            getClass().getResourceAsStream("/connection/obo/create-connection-obo-unauthorized.swadl.yaml"));

    when(bdkGateway.obo(any(Long.class))).thenThrow(new RuntimeException("Unauthorized user"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/create-connection-obo-unauthorized"));

    assertThat(workflow).executed("createConnectionOboUnauthorized").notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  @DisplayName(
      "Given an incoming connection request from a user, when the workflow is triggered,"
          + "then the connection is accepted")
  void acceptConnectionStatus() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/connection/accept-connection.swadl.yaml"));

    final Long userId = 1234L;
    final UserConnection userConnection = connection(userId, UserConnection.StatusEnum.ACCEPTED);

    when(connectionService.acceptConnection(userId)).thenReturn(userConnection);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/accept-connection"));

    verify(connectionService, timeout(5000)).acceptConnection(userId);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_CONNECTION_KEY, "acceptConnection"), userConnection);
  }

  @ParameterizedTest
  @CsvSource(
      {"/connection/obo/accept-connection-obo-valid-username.swadl.yaml, /accept-connection-obo-valid-username, "
          + "acceptConnectionOboValidUsername",
          "/connection/obo/accept-connection-obo-valid-userid.swadl.yaml, /accept-connection-obo-valid-userid, "
              + "acceptConnectionOboValidUserid"})
  void acceptConnectionStatusObo(String workflowFile, String command, String output) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    final Long userId = 1234L;
    final UserConnection userConnection = connection(userId, UserConnection.StatusEnum.ACCEPTED);

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);
    when(oboConnectionService.acceptConnection(userId)).thenReturn(userConnection);

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    verify(oboConnectionService, timeout(5000)).acceptConnection(userId);

    assertThat(workflow).isExecuted().hasOutput(String.format(OUTPUT_CONNECTION_KEY, output), userConnection);
  }

  @Test
  void acceptConnectionStatusOboUnauthorized() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/connection/obo/accept-connection-obo-unauthorized.swadl.yaml"));

    when(bdkGateway.obo(any(Long.class))).thenThrow(new RuntimeException("unauthorized user"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/accept-connection-obo-unauthorized"));

    assertThat(workflow).executed("acceptConnectionOboUnauthorized").notExecuted("scriptActivityNotToBeExecuted");

  }

  @Test
  @DisplayName(
      "Given an incoming connection request from a user, when the workflow is triggered,"
          + "then the connection is rejected")
  void rejectConnectionStatus() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/connection/reject-connection.swadl.yaml"));

    final Long userId = 1234L;
    final UserConnection userConnection = connection(userId, UserConnection.StatusEnum.REJECTED);

    when(connectionService.rejectConnection(userId)).thenReturn(userConnection);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/reject-connection"));

    verify(connectionService, timeout(5000)).rejectConnection(userId);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_CONNECTION_KEY, "rejectConnection"), userConnection);
  }

  @ParameterizedTest
  @CsvSource(
      {"/connection/obo/reject-connection-obo-valid-username.swadl.yaml, /reject-connection-obo-valid-username, "
          + "rejectConnectionOboValidUsername",
          "/connection/obo/reject-connection-obo-valid-userid.swadl.yaml, /reject-connection-obo-valid-userid, "
              + "rejectConnectionOboValidUserid"})
  void rejectConnectionStatusObo(String workflowFile, String command, String output) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    final Long userId = 1234L;
    final UserConnection userConnection = connection(userId, UserConnection.StatusEnum.REJECTED);

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);
    when(oboConnectionService.rejectConnection(userId)).thenReturn(userConnection);

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    verify(oboConnectionService, timeout(5000)).rejectConnection(userId);

    assertThat(workflow).isExecuted().hasOutput(String.format(OUTPUT_CONNECTION_KEY, output), userConnection);
  }

  @Test
  void rejectConnectionStatusOboUnauthorized() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/connection/obo/reject-connection-obo-unauthorized.swadl.yaml"));

    when(bdkGateway.obo(any(Long.class))).thenThrow(new RuntimeException("Unauthorized user"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/reject-connection-obo-unauthorized"));

    assertThat(workflow).executed("rejectConnectionOboUnauthorized").notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  @DisplayName(
      "Given connection request from a user, when the workflow is triggered, then the connection is removed")
  void removeConnectionStatus() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/connection/remove-connection.swadl.yaml"));

    final Long userId = 1234L;

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/remove-connection"));

    verify(connectionService, timeout(5000)).removeConnection(userId);

    assertThat(workflow).isExecuted();
  }

  @ParameterizedTest
  @CsvSource({"/connection/obo/remove-connection-obo-valid-username.swadl.yaml, /remove-connection-obo-valid-username",
      "/connection/obo/remove-connection-obo-valid-userid.swadl.yaml, /remove-connection-obo-valid-userid"})
  void removeConnectionStatusObo(String workflowFile, String command) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    final Long userId = 1234L;

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    verify(oboConnectionService, timeout(5000)).removeConnection(userId);

    assertThat(workflow).isExecuted();
  }

  @Test
  void removeConnectionStatusOboUnauthorized() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/connection/obo/remove-connection-obo-unauthorized.swadl.yaml"));

    when(bdkGateway.obo(any(Long.class))).thenThrow(new RuntimeException("Unauthorized user"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/remove-connection-obo-unauthorized"));

    assertThat(workflow).executed("removeConnectionOboUnauthorized").notExecuted("scriptActivityNotToBeExecuted");
  }
}
