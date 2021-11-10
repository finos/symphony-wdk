package com.symphony.bdk.workflow.engine.executor.user;

import com.symphony.bdk.core.service.user.UserService;
import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.user.GetUsers;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class GetUsersExecutor implements ActivityExecutor<GetUsers> {

  private static final String OUTPUT_USERS_KEY = "users";

  @Override
  public void execute(ActivityExecutorContext<GetUsers> context) {

    log.debug("Searching users");
    GetUsers getUsers = context.getActivity();

    List<UserV2> users = null;

    // Since the workflow is validated by swadl-schema, at least one of the following attributes is not null
    UserService userService = context.bdk().users();
    if (getUsers.getUsernames() != null) {
      users = userService.listUsersByUsernames(getUsers.getUsernames(), getUsers.getActive());

    } else if (getUsers.getUserIds() != null) {
      users = userService.listUsersByIds(getUsers.getUserIds(), getUsers.getLocal(),
          getUsers.getActive());

    } else if (getUsers.getEmails() != null) {
      users = userService.listUsersByEmails(getUsers.getEmails(), getUsers.getLocal(),
          getUsers.getActive());

    }

    context.setOutputVariable(OUTPUT_USERS_KEY, users);
  }

}
