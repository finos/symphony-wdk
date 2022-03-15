package com.symphony.bdk.workflow.engine.executor.group;

import com.symphony.bdk.ext.group.gen.api.model.BaseProfile;
import com.symphony.bdk.ext.group.gen.api.model.GroupImplicitConnection;
import com.symphony.bdk.ext.group.gen.api.model.GroupInteractionTransfer;
import com.symphony.bdk.ext.group.gen.api.model.GroupVisibilityRestriction;
import com.symphony.bdk.ext.group.gen.api.model.Member;
import com.symphony.bdk.ext.group.gen.api.model.Owner;
import com.symphony.bdk.ext.group.gen.api.model.ReadGroup;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.group.CreateGroup;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CreateGroupExecutor implements ActivityExecutor<CreateGroup> {

  private static final String OUTPUTS_GROUP_KEY = "group";

  @Override
  public void execute(ActivityExecutorContext<CreateGroup> execution) {

    com.symphony.bdk.ext.group.gen.api.model.CreateGroup group = toCreateGroup(execution.getActivity());
    ReadGroup createdGroup = execution.bdk().groups().insertGroup(group);

    log.debug("Group created with id {}", createdGroup.getId());
    execution.setOutputVariable(OUTPUTS_GROUP_KEY, createdGroup);
  }

  private com.symphony.bdk.ext.group.gen.api.model.CreateGroup toCreateGroup(CreateGroup createGroup) {
    return new com.symphony.bdk.ext.group.gen.api.model.CreateGroup().type(createGroup.getType())
        .ownerId(createGroup.getOwner().getId())
        .ownerType(Owner.fromValue(createGroup.getOwner().getType()))
        .name(createGroup.getName())
        .subType(toSubType(createGroup))
        .referrer(createGroup.getReferrer())
        .members(toMembers(createGroup.getMembers()))
        .profile(toProfile(createGroup.getProfile()))
        .visibilityRestriction(toVisibilityRestriction(createGroup.getVisibilityRestriction()))
        .implicitConnection(toImplicitConnection(createGroup.getImplicitConnection()))
        .interactionTransfer(toInteractionTransfer(createGroup.getInteractionTransfer()));
  }

  private com.symphony.bdk.ext.group.gen.api.model.CreateGroup.SubTypeEnum toSubType(CreateGroup createGroup) {
    if (createGroup.getSubType() == null) {
      return null;
    } else {
      return com.symphony.bdk.ext.group.gen.api.model.CreateGroup.SubTypeEnum.valueOf(createGroup.getSubType());
    }
  }

  static List<Member> toMembers(List<CreateGroup.GroupMember> members) {
    if (members == null) {
      return null;
    }
    return members.stream()
        .map(m -> new Member().memberId(m.getUserId()).memberTenant(m.getTenantId()))
        .collect(Collectors.toList());
  }

  static BaseProfile toProfile(CreateGroup.Profile profile) {
    if (profile == null) {
      return null;
    } else {
      BaseProfile baseProfile = new BaseProfile().displayName(profile.getDisplayName())
          .companyName(profile.getCompanyName())
          .email(profile.getEmail())
          .mobile(profile.getMobile())
          .industryOfInterest(profile.getIndustries())
          .assetClassesOfInterest(profile.getIndustries())
          .marketCoverage((profile.getMarketCoverages()))
          .responsibility((profile.getResponsibilities()))
          .function((profile.getFunctions()))
          .instrument((profile.getInstruments()));
      if (profile.getJob() != null) {
        baseProfile.jobTitle(profile.getJob().getTitle())
            .jobRole(profile.getJob().getRole())
            .jobDepartment(profile.getJob().getDepartment())
            .jobDivision(profile.getJob().getDivision())
            .jobPhone(profile.getJob().getDivision())
            .jobCity(profile.getJob().getCity());
      }
      return baseProfile;
    }
  }

  static GroupVisibilityRestriction toVisibilityRestriction(CreateGroup.VisibilityRestriction visibilityRestriction) {
    if (visibilityRestriction == null) {
      return null;
    } else {
      return new GroupVisibilityRestriction().restrictedTenantsList(visibilityRestriction.getTenantIds())
          .restrictedUsersList(visibilityRestriction.getUserIds());
    }
  }

  static GroupImplicitConnection toImplicitConnection(CreateGroup.ImplicitConnection implicitConnection) {
    if (implicitConnection == null) {
      return null;
    } else {
      return new GroupImplicitConnection()
          .connectedTenantsList(implicitConnection.getTenantIds())
          .connectedUsersList(implicitConnection.getUserIds());
    }
  }

  static GroupInteractionTransfer toInteractionTransfer(CreateGroup.InteractionTransfer interactionTransfer) {
    if (interactionTransfer == null) {
      return null;
    } else {
      return new GroupInteractionTransfer()
          .restrictedTenantsList(interactionTransfer.getTenantIds())
          .restrictedUsersList(interactionTransfer.getUserIds());
    }
  }
}
