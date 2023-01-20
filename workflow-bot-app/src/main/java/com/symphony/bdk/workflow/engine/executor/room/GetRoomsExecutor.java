package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.core.service.stream.OboStreamService;
import com.symphony.bdk.gen.api.model.UserId;
import com.symphony.bdk.gen.api.model.V2RoomSearchCriteria;
import com.symphony.bdk.gen.api.model.V3RoomSearchResults;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.room.GetRooms;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetRoomsExecutor extends OboExecutor<GetRooms, V3RoomSearchResults>
    implements ActivityExecutor<GetRooms> {

  private static final String OUTPUTS_ROOMS_KEY = "rooms";

  @Override
  public void execute(ActivityExecutorContext<GetRooms> execution) {
    log.debug("Getting rooms");

    GetRooms getRooms = execution.getActivity();
    V3RoomSearchResults rooms;

    if (this.isObo(getRooms)) {
      rooms = this.doOboWithCache(execution);
    } else if (getRooms.getLimit() != null && getRooms.getSkip() != null) {
      rooms = this.searchRoomsWithPagination(execution);
    } else if (getRooms.getLimit() == null && getRooms.getSkip() == null) {
      rooms = this.searchRoomsNoPagination(execution);
    } else {
      throw new IllegalArgumentException(
          String.format("Skip and limit should both be set in activity %s to get rooms", getRooms.getId()));
    }

    execution.setOutputVariable(OUTPUTS_ROOMS_KEY, rooms);
  }

  private V2RoomSearchCriteria toCriteria(GetRooms getRooms) {
    V2RoomSearchCriteria criteria = new V2RoomSearchCriteria()
        .query(getRooms.getQuery())
        .labels(getRooms.getLabels())
        ._private(getRooms.getIsPrivate())
        .active(getRooms.getActive());
    if (getRooms.getSortOrder() != null) {
      criteria.setSortOrder(V2RoomSearchCriteria.SortOrderEnum.fromValue(getRooms.getSortOrder()));
    }
    if (getRooms.getCreatorId() != null) {
      criteria.setCreator(new UserId().id(Long.parseLong(getRooms.getCreatorId())));
    }
    if (getRooms.getOwnerId() != null) {
      criteria.setOwner(new UserId().id(Long.parseLong(getRooms.getOwnerId())));
    }
    if (getRooms.getMemberId() != null) {
      criteria.setMember(new UserId().id(Long.parseLong(getRooms.getMemberId())));
    }
    return criteria;
  }

  private V3RoomSearchResults searchRoomsWithPagination(ActivityExecutorContext<GetRooms> execution) {
    GetRooms getRooms = execution.getActivity();
    return execution.bdk()
        .streams()
        .searchRooms(toCriteria(getRooms), new PaginationAttribute(getRooms.getSkip(), getRooms.getLimit()));
  }

  private V3RoomSearchResults searchRoomsNoPagination(ActivityExecutorContext<GetRooms> execution) {
    GetRooms getRooms = execution.getActivity();
    return execution.bdk()
        .streams()
        .searchRooms(toCriteria(getRooms));
  }


  @Override
  protected V3RoomSearchResults doOboWithCache(ActivityExecutorContext<GetRooms> execution) {
    GetRooms getRooms = execution.getActivity();
    AuthSession authSession = this.getOboAuthSession(execution);
    OboStreamService streamOboService = execution.bdk()
        .obo(authSession)
        .streams();

    if (getRooms.getLimit() != null && getRooms.getSkip() != null) {
      return streamOboService.searchRooms(toCriteria(getRooms),
          new PaginationAttribute(getRooms.getSkip(), getRooms.getLimit()));

    } else if (getRooms.getLimit() == null && getRooms.getSkip() == null) {
      return streamOboService.searchRooms(toCriteria(getRooms));
    } else {
      throw new IllegalArgumentException(
          String.format("Skip and limit should both be set to get rooms in activity %s", getRooms.getId()));
    }
  }
}
