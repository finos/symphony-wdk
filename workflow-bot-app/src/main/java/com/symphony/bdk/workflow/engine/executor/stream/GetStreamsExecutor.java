package com.symphony.bdk.workflow.engine.executor.stream;

import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.gen.api.model.V2AdminStreamFilter;
import com.symphony.bdk.gen.api.model.V2AdminStreamList;
import com.symphony.bdk.gen.api.model.V2AdminStreamType;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.DateTimeUtils;
import com.symphony.bdk.workflow.swadl.v1.activity.stream.GetStreams;

import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
public class GetStreamsExecutor implements ActivityExecutor<GetStreams> {

  private static final String OUTPUTS_STREAMS_KEY = "streams";

  @Override
  public void execute(ActivityExecutorContext<GetStreams> execution) {
    log.debug("Getting streams");

    GetStreams getStreams = execution.getActivity();
    V2AdminStreamList streams;
    if (getStreams.getLimit() != null && getStreams.getSkip() != null) {
      streams = execution.bdk().streams().listStreamsAdmin(toFilter(getStreams),
          new PaginationAttribute(getStreams.getSkip(), getStreams.getLimit()));
    } else if (getStreams.getLimit() == null && getStreams.getSkip() == null) {
      streams = execution.bdk().streams().listStreamsAdmin(toFilter(getStreams));
    } else {
      throw new IllegalArgumentException(
          String.format("Skip and limit should both be set to get streams %s", getStreams.getId()));
    }

    execution.setOutputVariable(OUTPUTS_STREAMS_KEY, streams);
  }

  private V2AdminStreamFilter toFilter(GetStreams getRooms) {
    V2AdminStreamFilter filter = new V2AdminStreamFilter()
        .scope(getRooms.getScope())
        .origin(getRooms.getOrigin())
        .status(getRooms.getStatus())
        .privacy(getRooms.getPrivacy())
        .startDate(DateTimeUtils.toEpochMilli(getRooms.getStartDate()))
        .endDate(DateTimeUtils.toEpochMilli(getRooms.getEndDate()));
    if (getRooms.getTypes() != null) {
      filter.setStreamTypes(
          getRooms.getTypes().stream()
              .map(t -> new V2AdminStreamType().type(t))
              .collect(Collectors.toList()));
    }
    return filter;
  }

}
