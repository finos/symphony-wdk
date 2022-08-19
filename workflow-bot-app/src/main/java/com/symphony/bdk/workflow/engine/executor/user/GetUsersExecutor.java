package com.symphony.bdk.workflow.engine.executor.user;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.user.GetUsers;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class GetUsersExecutor extends OboExecutor<GetUsers, List<UserV2>>
    implements ActivityExecutor<GetUsers> {

  private static final String OUTPUT_USERS_KEY = "users";

  @Override
  public void execute(ActivityExecutorContext<GetUsers> context) {

    log.debug("Searching users");
    GetUsers getUsers = context.getActivity();

    List<UserV2> users = null;

    if (this.isObo(getUsers)) {
      users = this.doOboWithCache(context);
    } else if (getUsers.getUsernames() != null) {
      // Since the workflow is validated by swadl-schema, at least one of the following attributes is not null
      users = context.bdk().users().listUsersByUsernames(getUsers.getUsernames(), getUsers.getActive());

    } else if (getUsers.getUserIds() != null) {
      users = context.bdk().users().listUsersByIds(getUsers.getUserIds(), getUsers.getLocal(), getUsers.getActive());

    } else if (getUsers.getEmails() != null) {
      users = context.bdk().users().listUsersByEmails(getUsers.getEmails(), getUsers.getLocal(), getUsers.getActive());
    }

    context.setOutputVariable(OUTPUT_USERS_KEY, users);
  }

  @Override
  protected List<UserV2> doOboWithCache(ActivityExecutorContext<GetUsers> execution) {
    GetUsers getUsers = execution.getActivity();
    AuthSession authSession = this.getOboAuthSession(execution);

    // Since the workflow is validated by swadl-schema, at least one of the following attributes is not null
    if (getUsers.getUsernames() != null) {
      return execution.bdk()
          .obo(authSession)
          .users()
          .listUsersByUsernames(getUsers.getUsernames(), getUsers.getActive());
    } else if (getUsers.getUserIds() != null) {
      return execution.bdk()
          .obo(authSession)
          .users()
          .listUsersByIds(getUsers.getUserIds(), getUsers.getLocal(), getUsers.getActive());
    } else if (getUsers.getEmails() != null) {
      return execution.bdk()
          .obo(authSession)
          .users()
          .listUsersByEmails(getUsers.getEmails(), getUsers.getLocal(), getUsers.getActive());
    }

    return Collections.emptyList();
  }

}
