package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4MessageSuppressed;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4MessageSuppressedProcessor extends AbstractRealTimeEventProcessor<V4MessageSuppressed> {

  public V4MessageSuppressedProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.MESSAGE_SUPPRESSED.getEventName());
  }
}
