package com.symphony.bdk.workflow.engine.executor.stream;

import com.symphony.bdk.gen.api.model.V2StreamAttributes;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.stream.GetStream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetStreamExecutor implements ActivityExecutor<GetStream> {

  private static final String OUTPUTS_STREAM_KEY = "stream";

  @Override
  public void execute(ActivityExecutorContext<GetStream> execution) {
    String streamId = execution.getActivity().getStreamId();
    log.debug("Getting stream {}", streamId);
    V2StreamAttributes roomInfo = execution.streams().getStream(streamId);
    execution.setOutputVariable(OUTPUTS_STREAM_KEY, roomInfo);
  }

}
