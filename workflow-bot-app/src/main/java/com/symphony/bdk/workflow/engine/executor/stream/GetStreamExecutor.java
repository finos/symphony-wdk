package com.symphony.bdk.workflow.engine.executor.stream;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.gen.api.model.V2StreamAttributes;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.stream.GetStream;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;

@Slf4j
public class GetStreamExecutor extends OboExecutor<GetStream, V2StreamAttributes>
    implements ActivityExecutor<GetStream> {

  private static final String OUTPUTS_STREAM_KEY = "stream";

  @Override
  public void execute(ActivityExecutorContext<GetStream> execution) {
    String streamId = execution.getActivity().getStreamId();
    log.debug("Getting stream {}", streamId);

    V2StreamAttributes roomInfo;
    if (this.isObo(execution.getActivity())) {
      roomInfo = this.doOboWithCache(execution);
    } else {
      roomInfo = execution.bdk().streams().getStream(streamId);
    }
    execution.setOutputVariable(OUTPUTS_STREAM_KEY, roomInfo);
  }

  @Override
  protected V2StreamAttributes doOboWithCache(ActivityExecutorContext<GetStream> execution) {
    AuthSession authSession = this.getOboAuthSession(execution);
    return execution.bdk().obo(authSession).streams().getStream(execution.getActivity().getStreamId());
  }

}
