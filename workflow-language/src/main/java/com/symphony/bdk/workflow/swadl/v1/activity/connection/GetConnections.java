package com.symphony.bdk.workflow.swadl.v1.activity.connection;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
import com.symphony.bdk.workflow.swadl.v1.activity.Obo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#list-connections">List Connections API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetConnections extends BaseActivity {
  private List<Long> userIds;
  @Nullable private String status;
  private Obo obo;
}
