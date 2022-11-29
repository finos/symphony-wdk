package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4ConnectionAccepted;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4ConnectionAcceptedProcessor extends AbstractRealTimeEventProcessor<V4ConnectionAccepted> {

  public V4ConnectionAcceptedProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.CONNECTION_ACCEPTED.getEventName());
  }

}
