package com.symphony.bdk.workflow.engine.executor.message;

import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.core.service.stream.util.StreamUtil;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.message.GetMessages;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
public class GetMessagesExecutor implements ActivityExecutor<GetMessages> {

  private static final String OUTPUT_MESSAGES_KEY = "messages";

  @Override
  public void execute(
      ActivityExecutorContext<GetMessages> context) {
    GetMessages activity = context.getActivity();
    String streamId = activity.getStreamId();

    // TODO remove once https://github.com/finos/symphony-bdk-java/pull/567 is released
    if (streamId != null && streamId.endsWith("=")) {
      streamId = StreamUtil.toUrlSafeStreamId(streamId);
    }

    log.debug("Get messages by stream id {} since {}", streamId, activity.getSince());

    if (streamId != null && activity.getSince() != null) {
      PaginationAttribute pagination = this.buildPagination(activity.getSkip().get(), activity.getLimit().get());

      if (pagination != null) {
        context.setOutputVariable(OUTPUT_MESSAGES_KEY,
            context.bdk().messages().listMessages(streamId, Instant.parse(activity.getSince()), pagination));
      } else {
        context.setOutputVariable(OUTPUT_MESSAGES_KEY,
            context.bdk().messages().listMessages(streamId, Instant.parse(activity.getSince())));
      }
    }
  }

  private PaginationAttribute buildPagination(Number skip, Number limit) {
    if (skip != null && limit != null) {
      return new PaginationAttribute(skip.intValue(), limit.intValue());
    }
    return null;
  }

}
