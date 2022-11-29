package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4InstantMessageCreated;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4InstantMessageCreatedProcessor extends AbstractRealTimeEventProcessor<V4InstantMessageCreated> {

  public V4InstantMessageCreatedProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.IM_CREATED.getEventName());
  }
}
