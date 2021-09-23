package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.gen.api.model.UserId;
import com.symphony.bdk.gen.api.model.V2RoomSearchCriteria;
import com.symphony.bdk.gen.api.model.V3RoomSearchResults;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.GetRooms;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetRoomsExecutor implements ActivityExecutor<GetRooms> {

  private static final String OUTPUTS_ROOMS_KEY = "rooms";

  @Override
  public void execute(ActivityExecutorContext<GetRooms> execution) {
    log.debug("Getting rooms");

    GetRooms getRooms = execution.getActivity();
    V3RoomSearchResults rooms;
    if (getRooms.getLimit() != null && getRooms.getSkip() != null) {
      rooms = execution.bdk().streams().searchRooms(toCriteria(getRooms),
          new PaginationAttribute(getRooms.getSkip().getInt(), getRooms.getLimit().getInt()));
    } else if (getRooms.getLimit() == null && getRooms.getSkip() == null) {
      rooms = execution.bdk().streams().searchRooms(toCriteria(getRooms));
    } else {
      throw new IllegalArgumentException(
          String.format("Skip and limit should both be set to get rooms in activity %s", getRooms.getId()));
    }

    execution.setOutputVariable(OUTPUTS_ROOMS_KEY, rooms);
  }

  private V2RoomSearchCriteria toCriteria(GetRooms getRooms) {
    V2RoomSearchCriteria criteria = new V2RoomSearchCriteria()
        .query(getRooms.getQuery())
        .labels(getRooms.getLabels().get())
        ._private(getRooms.getIsPrivate().get())
        .active(getRooms.getActive().get());
    if (getRooms.getSortOrder() != null) {
      criteria.setSortOrder(V2RoomSearchCriteria.SortOrderEnum.fromValue(getRooms.getSortOrder()));
    }
    if (getRooms.getCreatorId() != null) {
      criteria.setCreator(new UserId().id(Long.parseLong(getRooms.getCreatorId())));
    }
    if (getRooms.getOwnerId() != null) {
      criteria.setOwner(new UserId().id(Long.parseLong(getRooms.getCreatorId())));
    }
    if (getRooms.getMemberId() != null) {
      criteria.setMember(new UserId().id(Long.parseLong(getRooms.getCreatorId())));
    }
    return criteria;
  }

}
