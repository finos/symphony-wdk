package com.symphony.bdk.workflow.engine.executor.user;

import com.symphony.bdk.core.service.user.UserService;
import com.symphony.bdk.gen.api.model.UserStatus;
import com.symphony.bdk.gen.api.model.V2UserDetail;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.user.CreateUser;
import com.symphony.bdk.workflow.swadl.v1.activity.user.UpdateUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateUserExecutor implements ActivityExecutor<UpdateUser> {

  private static final String OUTPUT_USER_KEY = "user";

  @Override
  public void execute(ActivityExecutorContext<UpdateUser> context) {
    doExecute(context);
  }

  // to support subtypes of UpdateUser
  void doExecute(ActivityExecutorContext<? extends UpdateUser> context) {
    UpdateUser updateUser = context.getActivity();
    UserService userService = context.bdk().users();

    Long userId = Long.valueOf(updateUser.getUserId());

    // we might use the activity with only entitlements or status being set
    if (shouldUpdateUser(updateUser)) {
      log.debug("Updating user {}", userId);
      userService.update(userId, CreateUserExecutor.toUserAttributes(updateUser));
    }

    if (updateUser.getEntitlements() != null && !updateUser.getEntitlements().isEmpty()) {
      log.debug("Updating entitlements for user {}", userId);
      userService.updateFeatureEntitlements(userId, CreateUserExecutor.toFeatures(updateUser.getEntitlements()));
    }

    if (updateUser.getStatus() != null) {
      log.debug("Updating status for user {}", userId);
      userService.updateStatus(userId,
          new UserStatus().status(UserStatus.StatusEnum.fromValue(updateUser.getStatus())));
    }

    // in case status or entitlements were updated, fetch again the user
    V2UserDetail userDetail = userService.getUserDetail(userId);
    context.setOutputVariable(OUTPUT_USER_KEY, userDetail);
  }

  private boolean shouldUpdateUser(UpdateUser updateUser) {
    return updateUser.getEmail() != null
        || updateUser.getFirstname() != null
        || updateUser.getLastname() != null
        || updateUser.getDisplayName() != null
        || updateUser.getRecommendedLanguage() != null
        || shouldUpdateContact(updateUser.getContact())
        || shouldUpdateBusiness(updateUser.getBusiness());
  }

  private boolean shouldUpdateContact(CreateUser.Contact contact) {
    return contact != null
        && contact.getSmsNumber() != null
        && contact.getTwoFactorAuthNumber() != null
        && contact.getMobilePhoneNumber() != null
        && contact.getWorkPhoneNumber() != null;
  }

  private boolean shouldUpdateBusiness(CreateUser.Business business) {
    return business != null
        && business.getDepartment() != null
        && business.getCompanyName() != null
        && business.getAssetClasses() != null
        && business.getDivision() != null
        && business.getFunctions() != null
        && business.getIndustries() != null
        && business.getInstruments() != null
        && business.getLocation() != null
        && business.getJobFunction() != null
        && business.getMarketCoverages() != null
        && business.getTitle() != null
        && business.getResponsibilities() != null;
  }

}
