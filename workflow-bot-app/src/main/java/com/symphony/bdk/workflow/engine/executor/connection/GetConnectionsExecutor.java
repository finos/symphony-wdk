package com.symphony.bdk.workflow.engine.executor.connection;

import com.symphony.bdk.core.service.connection.constant.ConnectionStatus;
import com.symphony.bdk.gen.api.model.UserConnection;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.connection.GetConnections;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GetConnectionsExecutor implements ActivityExecutor<GetConnections> {

  private static final String OUTPUT_CONNECTIONS_KEY = "connections";

  @Override
  public void execute(ActivityExecutorContext<GetConnections> context) {
    GetConnections activity = context.getActivity();

    List<UserConnection> connections = context.bdk().connections()
        .listConnections(toConnectionStatus(activity.getStatus()), toLongs(activity.getUserIds().get()));
    context.setOutputVariable(OUTPUT_CONNECTIONS_KEY, connections);
  }

  private ConnectionStatus toConnectionStatus(String statusString) {
    if (statusString == null) {
      return null;
    }

    return ConnectionStatus.valueOf(statusString);
  }

  private static List<Long> toLongs(List<Number> ids) {
    if (ids == null) {
      return null;
    }
    return ids.stream().map(Number::longValue).collect(Collectors.toList());
  }
}
