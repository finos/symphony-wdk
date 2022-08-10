package com.symphony.bdk.workflow.engine.executor.user;

import com.symphony.bdk.core.auth.AuthSession;
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
    if (getUsers.getUsernames() != null) {
      users = this.listUsersByUsernames(context);

    } else if (getUsers.getUserIds() != null) {
      users = this.listUsersByIds(context);

    } else if (getUsers.getEmails() != null) {
      users = this.listUsersByEmails(context);
    }

    context.setOutputVariable(OUTPUT_USERS_KEY, users);
  }

  private boolean isObo(GetUsers activity) {
    return activity.getObo() != null && (activity.getObo().getUsername() != null
        || activity.getObo().getUserId() != null);
  }

  private List<UserV2> listUsersByUsernames(ActivityExecutorContext<GetUsers> context) {
    GetUsers activity = context.getActivity();

    if (this.isObo(activity)) {
      AuthSession authSession = this.getAuthSession(context);

      return context.bdk().obo(authSession).users().listUsersByUsernames(activity.getUsernames(), activity.getActive());
    } else {
      return context.bdk().users().listUsersByUsernames(activity.getUsernames(), activity.getActive());
    }
  }

  private List<UserV2> listUsersByIds(ActivityExecutorContext<GetUsers> context) {
    GetUsers activity = context.getActivity();

    if (this.isObo(activity)) {
      AuthSession authSession = this.getAuthSession(context);

      return context.bdk()
          .obo(authSession)
          .users()
          .listUsersByIds(activity.getUserIds(), activity.getLocal(), activity.getActive());
    } else {
      return context.bdk().users().listUsersByIds(activity.getUserIds(), activity.getLocal(), activity.getActive());
    }
  }

  private List<UserV2> listUsersByEmails(ActivityExecutorContext<GetUsers> context) {
    GetUsers activity = context.getActivity();

    if (this.isObo(activity)) {
      AuthSession authSession = this.getAuthSession(context);

      return context.bdk()
          .obo(authSession)
          .users()
          .listUsersByEmails(activity.getEmails(), activity.getLocal(), activity.getActive());
    } else {
      return context.bdk().users().listUsersByEmails(activity.getEmails(), activity.getLocal(), activity.getActive());
    }
  }

  private AuthSession getAuthSession(ActivityExecutorContext<GetUsers> context) {
    AuthSession authSession;
    GetUsers activity = context.getActivity();

    if (activity.getObo().getUsername() != null) {
      authSession = context.bdk().obo(activity.getObo().getUsername());
    } else {
      authSession = context.bdk().obo(activity.getObo().getUserId());
    }

    return authSession;
  }

}
