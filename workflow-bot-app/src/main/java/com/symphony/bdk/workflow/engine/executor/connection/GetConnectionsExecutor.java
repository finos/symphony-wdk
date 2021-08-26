package com.symphony.bdk.workflow.engine.executor.connection;

import com.symphony.bdk.core.service.connection.constant.ConnectionStatus;
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
  public void execute(
      ActivityExecutorContext<GetConnections> context) {
    GetConnections activity = context.getActivity();

    context.setOutputVariable(OUTPUT_CONNECTIONS_KEY, context.bdk().connections()
        .listConnections(ConnectionStatus.valueOf(activity.getStatus()), toLongs(activity.getUserIds())));
  }

  private List<Long> toLongs(List<String> ids) {
    return ids.stream().map(Long::parseLong).collect(Collectors.toList());
  }
}
