package com.symphony.bdk.workflow.swadl.v1.activity.group;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference/addmembertogroup">Add member to group API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AddGroupMember extends BaseActivity {
  private String groupId;
  private List<CreateGroup.GroupMember> members = List.of();
}
