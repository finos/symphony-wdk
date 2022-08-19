package com.symphony.bdk.workflow.swadl.v1.activity.stream;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
import com.symphony.bdk.workflow.swadl.v1.activity.Obo;
import com.symphony.bdk.workflow.swadl.v1.activity.OboActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#list-user-streams">Get user streams API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetUserStreams extends OboActivity {
  private List<String> types;
  private Boolean includeInactiveStreams;
  private Integer limit;
  private Integer skip;

}
