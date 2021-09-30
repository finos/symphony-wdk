package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.connection.constant.ConnectionStatus;
import com.symphony.bdk.gen.api.model.UserConnection;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

    engine.deploy(workflow, "defaultId");
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

    engine.deploy(workflow, "defaultId");
    engine.onEvent(messageReceived("/get-connections-no-params"));

    verify(connectionService, timeout(5000)).listConnections(null, null);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_CONNECTIONS_KEY, "getConnections"), userConnections);
  }

  @Test
  @DisplayName("Given a connection with a user, when the workflow is triggered, then the connection is returned")
  void getConnectionStatus() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/connection/get-connection.swadl.yaml"));

    final Long userId = 1234L;
    final UserConnection userConnection = connection(userId);

    when(connectionService.getConnection(userId)).thenReturn(userConnection);

    engine.deploy(workflow, "defaultId");
    engine.onEvent(messageReceived("/get-connection"));

    verify(connectionService, timeout(5000)).getConnection(userId);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_CONNECTION_KEY, "getConnection"), userConnection);
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

    engine.deploy(workflow, "defaultId");
    engine.onEvent(messageReceived("/create-connection"));

    verify(connectionService, timeout(5000)).createConnection(userId);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_CONNECTION_KEY, "createConnection"), userConnection);
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

    engine.deploy(workflow, "defaultId");
    engine.onEvent(messageReceived("/accept-connection"));

    verify(connectionService, timeout(5000)).acceptConnection(userId);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_CONNECTION_KEY, "acceptConnection"), userConnection);
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

    engine.deploy(workflow, "defaultId");
    engine.onEvent(messageReceived("/reject-connection"));

    verify(connectionService, timeout(5000)).rejectConnection(userId);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_CONNECTION_KEY, "rejectConnection"), userConnection);
  }

  @Test
  @DisplayName(
      "Given connection request from a user, when the workflow is triggered, then the connection is removed")
  void removeConnectionStatus() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/connection/remove-connection.swadl.yaml"));

    final Long userId = 1234L;

    engine.deploy(workflow, "defaultId");
    engine.onEvent(messageReceived("/remove-connection"));

    verify(connectionService, timeout(5000)).removeConnection(userId);

    assertThat(workflow).isExecuted();
  }

}
