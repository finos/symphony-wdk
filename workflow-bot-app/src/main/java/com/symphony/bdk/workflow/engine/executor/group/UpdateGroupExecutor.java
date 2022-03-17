package com.symphony.bdk.workflow.engine.executor.group;

import static com.symphony.bdk.workflow.engine.executor.group.CreateGroupExecutor.toImplicitConnection;
import static com.symphony.bdk.workflow.engine.executor.group.CreateGroupExecutor.toInteractionTransfer;
import static com.symphony.bdk.workflow.engine.executor.group.CreateGroupExecutor.toMembers;
import static com.symphony.bdk.workflow.engine.executor.group.CreateGroupExecutor.toProfile;
import static com.symphony.bdk.workflow.engine.executor.group.CreateGroupExecutor.toVisibilityRestriction;

import com.symphony.bdk.ext.group.gen.api.model.Owner;
import com.symphony.bdk.ext.group.gen.api.model.ReadGroup;
import com.symphony.bdk.ext.group.gen.api.model.Status;
import com.symphony.bdk.ext.group.gen.api.model.UploadAvatar;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.group.CreateGroup;
import com.symphony.bdk.workflow.swadl.v1.activity.group.UpdateGroup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@Slf4j
public class UpdateGroupExecutor implements ActivityExecutor<UpdateGroup> {

  private static final String OUTPUTS_GROUP_KEY = "group";

  @Override
  public void execute(ActivityExecutorContext<UpdateGroup> execution) throws IOException {

    ReadGroup updatedGroup = null;
    if (execution.getActivity().getImagePath() != null) {
      try (InputStream image = execution.getResource(Path.of(execution.getActivity().getImagePath()))) {
        updatedGroup = execution.bdk().groups().updateAvatar(execution.getActivity().getGroupId(),
            new UploadAvatar().image(
                IOUtils.toByteArray(image)));
        // update etag in case we want to update other fields too
        execution.getActivity().setEtag(updatedGroup.geteTag());
      }
      log.debug("Group avatar {} updated", execution.getActivity().getGroupId());
    }

    if (execution.getActivity().getStatus() != null) { // required field in case an update is performed
      com.symphony.bdk.ext.group.gen.api.model.UpdateGroup group = toUpdateGroup(execution.getActivity());
      updatedGroup = execution.bdk().groups().updateGroup(
          execution.getActivity().getEtag(),
          execution.getActivity().getGroupId(), group);
      log.debug("Group {} updated", execution.getActivity().getGroupId());
    }

    if (updatedGroup != null) {
      execution.setOutputVariable(OUTPUTS_GROUP_KEY, updatedGroup);
    }
  }

  private com.symphony.bdk.ext.group.gen.api.model.UpdateGroup toUpdateGroup(UpdateGroup group) {
    com.symphony.bdk.ext.group.gen.api.model.UpdateGroup updateGroup =
        new com.symphony.bdk.ext.group.gen.api.model.UpdateGroup().type(group.getType())
            .id(group.getGroupId())
            .eTag(group.getEtag())
            .name(group.getName())
            .status(Status.valueOf(group.getStatus()))
            .subType(toSubType(group))
            .referrer(group.getReferrer())
            .members(toMembers(group.getMembers()))
            .profile(toProfile(group.getProfile()))
            .visibilityRestriction(toVisibilityRestriction(group.getVisibilityRestriction()))
            .implicitConnection(toImplicitConnection(group.getImplicitConnection()))
            .interactionTransfer(toInteractionTransfer(group.getInteractionTransfer()));
    if (group.getOwner() != null) {
      updateGroup.ownerId(group.getOwner().getId())
          .ownerType(Owner.fromValue(group.getOwner().getType()));
    }
    return updateGroup;
  }

  private com.symphony.bdk.ext.group.gen.api.model.UpdateGroup.SubTypeEnum toSubType(CreateGroup createGroup) {
    if (createGroup.getSubType() == null) {
      return null;
    } else {
      return com.symphony.bdk.ext.group.gen.api.model.UpdateGroup.SubTypeEnum.valueOf(createGroup.getSubType());
    }
  }

}
