package com.symphony.bdk.workflow.engine.executor.user;

import com.symphony.bdk.core.service.user.UserService;
import com.symphony.bdk.gen.api.model.Feature;
import com.symphony.bdk.gen.api.model.Password;
import com.symphony.bdk.gen.api.model.UserStatus;
import com.symphony.bdk.gen.api.model.V2UserAttributes;
import com.symphony.bdk.gen.api.model.V2UserCreate;
import com.symphony.bdk.gen.api.model.V2UserDetail;
import com.symphony.bdk.gen.api.model.V2UserKeyRequest;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.DateTimeUtils;
import com.symphony.bdk.workflow.swadl.v1.activity.user.CreateUser;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CreateUserExecutor implements ActivityExecutor<CreateUser> {

  private static final String OUTPUT_USER_KEY = "user";

  @Override
  public void execute(ActivityExecutorContext<CreateUser> context) {
    doExecute(context);
  }

  // to support subtypes of CreateUser
  void doExecute(ActivityExecutorContext<? extends CreateUser> context) {
    CreateUser createUser = context.getActivity();
    UserService userService = context.bdk().users();

    log.debug("Creating user");
    V2UserDetail createdUser = userService.create(toUser(createUser));

    Long userId = createdUser.getUserSystemInfo().getId();
    if (createUser.getEntitlements() != null && !createUser.getEntitlements().get().isEmpty()) {
      log.debug("Updating entitlements for user {}", userId);
      userService.updateFeatureEntitlements(userId, toFeatures(createUser.getEntitlements().get()));
    }

    if (createUser.getStatus() != null) {
      log.debug("Updating status for user {}", userId);
      userService.updateStatus(userId,
          new UserStatus().status(UserStatus.StatusEnum.fromValue(createUser.getStatus())));
    }

    // in case status or entitlements were updated, fetch again the user
    V2UserDetail userDetail = userService.getUserDetail(userId);
    context.setOutputVariable(OUTPUT_USER_KEY, userDetail);
  }

  private V2UserCreate toUser(CreateUser createUser) {
    V2UserCreate user = new V2UserCreate();

    V2UserAttributes attributes = toUserAttributes(createUser);

    user.setUserAttributes(attributes);

    if (createUser.getPassword() != null) {
      user.password(new Password()
          .hPassword(createUser.getPassword().getHashedPassword())
          .hSalt(createUser.getPassword().getHashedSalt())
          .khPassword(createUser.getPassword().getHashedKmPassword())
          .khSalt(createUser.getPassword().getHashedKmSalt())
      );
    }

    user.setRoles(createUser.getRoles().get());
    return user;
  }

  static V2UserAttributes toUserAttributes(CreateUser createUser) {
    V2UserAttributes attributes = new V2UserAttributes();
    attributes.setAccountType(V2UserAttributes.AccountTypeEnum.fromValue(createUser.getType()));
    attributes.setEmailAddress(createUser.getEmail());
    attributes.setUserName(createUser.getUsername());
    attributes.setFirstName(createUser.getFirstname());
    attributes.setLastName(createUser.getLastname());
    attributes.setDisplayName(createUser.getDisplayName());
    attributes.setRecommendedLanguage(createUser.getRecommendedLanguage());

    if (createUser.getContact() != null) {
      attributes.setWorkPhoneNumber(createUser.getContact().getWorkPhoneNumber());
      attributes.setMobilePhoneNumber(createUser.getContact().getMobilePhoneNumber());
      attributes.setTwoFactorAuthPhone(createUser.getContact().getTwoFactorAuthNumber());
      attributes.setSmsNumber(createUser.getContact().getSmsNumber());
    }

    if (createUser.getBusiness() != null) {
      attributes.setLocation(createUser.getBusiness().getLocation());
      attributes.setDepartment(createUser.getBusiness().getDepartment());
      attributes.setDivision(createUser.getBusiness().getDivision());
      attributes.setCompanyName(createUser.getBusiness().getCompanyName());
      attributes.setJobFunction(createUser.getBusiness().getJobFunction());
      attributes.setTitle(createUser.getBusiness().getTitle());
      attributes.setAssetClasses(createUser.getBusiness().getAssetClasses().get());
      attributes.setFunction(createUser.getBusiness().getFunctions().get());
      attributes.setIndustries(createUser.getBusiness().getIndustries().get());
      attributes.setInstrument(createUser.getBusiness().getInstruments().get());
      attributes.setResponsibility(createUser.getBusiness().getResponsibilities().get());
      attributes.setMarketCoverage(createUser.getBusiness().getMarketCoverages().get());
    }

    if (createUser.getKeys() != null) {
      if (createUser.getKeys().getCurrent() != null) {
        attributes.setCurrentKey(new V2UserKeyRequest()
            .key(createUser.getKeys().getCurrent().getKey())
            .action(createUser.getKeys().getCurrent().getAction())
            .expirationDate(DateTimeUtils.toEpochMilli(createUser.getKeys().getCurrent().getExpiration()))
        );
      }
      if (createUser.getKeys().getPrevious() != null) {
        attributes.setPreviousKey(new V2UserKeyRequest()
            .key(createUser.getKeys().getPrevious().getKey())
            .action(createUser.getKeys().getPrevious().getAction())
            .expirationDate(DateTimeUtils.toEpochMilli(createUser.getKeys().getPrevious().getExpiration()))
        );
      }
    }

    return attributes;
  }

  static List<Feature> toFeatures(Map<String, Boolean> entitlements) {
    return entitlements.entrySet().stream()
        .map(e -> new Feature().entitlment(e.getKey()).enabled(e.getValue()))
        .collect(Collectors.toList());
  }
}
