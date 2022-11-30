package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4SharedPost;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

@Service
public class V4SharedPostProcessor extends AbstractRealTimeEventProcessor<V4SharedPost> {

  public V4SharedPostProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.POST_SHARED.getEventName());
  }
}
