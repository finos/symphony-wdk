package com.symphony.bdk.workflow.swadl.v1.activity.group;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference/listgroups">Get groups API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetGroups extends BaseActivity {
  private String type = "SDL";
  @Nullable private String status;
  @Nullable private String before;
  @Nullable private String after;
  @Nullable private Integer limit = 100;
  @Nullable private String sortOrder = "ASC"; // DESC
}
