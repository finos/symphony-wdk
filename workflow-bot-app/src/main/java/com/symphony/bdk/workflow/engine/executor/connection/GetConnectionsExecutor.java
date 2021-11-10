package com.symphony.bdk.workflow.engine.executor.connection;

import com.symphony.bdk.core.service.connection.constant.ConnectionStatus;
import com.symphony.bdk.gen.api.model.UserConnection;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.connection.GetConnections;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class GetConnectionsExecutor implements ActivityExecutor<GetConnections> {

  private static final String OUTPUT_CONNECTIONS_KEY = "connections";

  @Override
  public void execute(ActivityExecutorContext<GetConnections> context) {
    GetConnections activity = context.getActivity();

    List<UserConnection> connections = context.bdk().connections()
        .listConnections(toConnectionStatus(activity.getStatus()), activity.getUserIds());
    context.setOutputVariable(OUTPUT_CONNECTIONS_KEY, connections);
  }

  private ConnectionStatus toConnectionStatus(String statusString) {
    if (statusString == null) {
      return null;
    }

    return ConnectionStatus.valueOf(statusString);
  }

}
