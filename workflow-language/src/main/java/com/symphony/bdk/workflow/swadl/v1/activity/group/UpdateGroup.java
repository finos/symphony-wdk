package com.symphony.bdk.workflow.swadl.v1.activity.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference/updategroup">Update group API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateGroup extends CreateGroup {
  private String groupId;
  private String status;
  @JsonProperty("e-tag")
  private String etag;

  private String imagePath;
}
