package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4ConnectionRequested;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4ConnectionRequestedProcessor extends AbstractRealTimeEventProcessor<V4ConnectionRequested> {

  public V4ConnectionRequestedProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.CONNECTION_REQUESTED.getEventName());
  }
}
