package com.symphony.bdk.workflow.engine.executor.stream;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.core.service.stream.OboStreamService;
import com.symphony.bdk.gen.api.model.StreamAttributes;
import com.symphony.bdk.gen.api.model.StreamFilter;
import com.symphony.bdk.gen.api.model.StreamType;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.stream.GetUserStreams;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GetUserStreamsExecutor extends OboExecutor<GetUserStreams, List<StreamAttributes>>
    implements ActivityExecutor<GetUserStreams> {

  private static final String OUTPUTS_STREAMS_KEY = "streams";

  @Override
  public void execute(ActivityExecutorContext<GetUserStreams> execution) {
    log.debug("Getting user streams");

    GetUserStreams getUserStreams = execution.getActivity();
    List<StreamAttributes> userStreams;
    if (this.isObo(getUserStreams)) {
      userStreams = this.doOboWithCache(execution);
    } else if (getUserStreams.getLimit() != null && getUserStreams.getSkip() != null) {
      userStreams = this.listUserStreamsWithPagination(execution);
    } else {
      userStreams = this.listUserStreamsNoPagination(execution);
    }

    execution.setOutputVariable(OUTPUTS_STREAMS_KEY, userStreams);
  }

  private StreamFilter toFilter(GetUserStreams getUserStreams) {
    StreamFilter filter = new StreamFilter()
        .includeInactiveStreams(getUserStreams.getIncludeInactiveStreams());

    if (getUserStreams.getTypes() != null) {
      filter.setStreamTypes(getUserStreams.getTypes().stream()
          .map(t -> new StreamType().type(StreamType.TypeEnum.fromValue(t)))
          .collect(Collectors.toList()));
    }
    return filter;
  }

  private List<StreamAttributes> listUserStreamsWithPagination(ActivityExecutorContext<GetUserStreams> execution) {
    GetUserStreams getUserStreams = execution.getActivity();
    return execution.bdk()
        .streams()
        .listStreams(toFilter(getUserStreams),
            new PaginationAttribute(getUserStreams.getSkip(), getUserStreams.getLimit()));
  }

  private List<StreamAttributes> listUserStreamsNoPagination(ActivityExecutorContext<GetUserStreams> execution) {
    GetUserStreams getUserStreams = execution.getActivity();
    return execution.bdk()
        .streams()
        .listStreams(toFilter(getUserStreams));
  }

  @Override
  protected List<StreamAttributes> doOboWithCache(
      ActivityExecutorContext<GetUserStreams> execution) {
    GetUserStreams getUserStreams = execution.getActivity();
    AuthSession authSession = this.getOboAuthSession(execution);
    OboStreamService streamOboService = execution.bdk()
        .obo(authSession)
        .streams();

    if (getUserStreams.getLimit() != null && getUserStreams.getSkip() != null) {
      return streamOboService.listStreams(toFilter(getUserStreams),
          new PaginationAttribute(getUserStreams.getSkip(), getUserStreams.getLimit()));
    } else {
      return streamOboService.listStreams(toFilter(getUserStreams));
    }
  }
}
