package com.symphony.bdk.workflow.engine.executor.user;

import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.user.GetUsers;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GetUsersExecutor implements ActivityExecutor<GetUsers> {

  private static final String OUTPUT_USERS_KEY = "users";

  @Override
  public void execute(ActivityExecutorContext<GetUsers> context) {

    log.debug("Searching users");
    GetUsers getUsers = context.getActivity();

    List<UserV2> users;
    if (getUsers.getUsernames() != null) {
      users = context.bdk().users().listUsersByUsernames(getUsers.getUsernames(), getUsers.getActiveAsBool());

    } else if (getUsers.getUserIds() != null) {
      users = context.bdk()
          .users()
          .listUsersByIds(toLongs(getUsers.getUserIds()), getUsers.getLocalAsBool(), getUsers.getActiveAsBool());

    } else if (getUsers.getEmails() != null) {
      users = context.bdk()
          .users()
          .listUsersByEmails(getUsers.getEmails(), getUsers.getLocalAsBool(), getUsers.getActiveAsBool());

    } else {
      throw new IllegalArgumentException("Usernames or user ids or emails must be set");
    }

    context.setOutputVariable(OUTPUT_USERS_KEY, users);
  }

  private List<Long> toLongs(List<String> ids) {
    return ids.stream().map(Long::parseLong).collect(Collectors.toList());
  }

}
