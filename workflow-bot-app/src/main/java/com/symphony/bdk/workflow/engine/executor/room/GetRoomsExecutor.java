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
    if (getRooms.getLimitAsInt() != null && getRooms.getSkipAsInt() != null) {
      rooms = execution.bdk().streams().searchRooms(toCritera(getRooms),
          new PaginationAttribute(getRooms.getSkipAsInt(), getRooms.getLimitAsInt()));
    } else if (getRooms.getLimitAsInt() == null && getRooms.getSkipAsInt() == null) {
      rooms = execution.bdk().streams().searchRooms(toCritera(getRooms));
    } else {
      throw new IllegalArgumentException("skip and limit should both be set to get rooms");
    }

    execution.setOutputVariable(OUTPUTS_ROOMS_KEY, rooms);
  }

  private V2RoomSearchCriteria toCritera(GetRooms getRooms) {
    V2RoomSearchCriteria criteria = new V2RoomSearchCriteria()
        .query(getRooms.getQuery())
        .labels(getRooms.getLabels())
        ._private(getRooms.getIsPrivateAsBool())
        .active(getRooms.getActiveAsBool());
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
