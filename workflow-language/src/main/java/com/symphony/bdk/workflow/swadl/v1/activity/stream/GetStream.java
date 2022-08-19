package com.symphony.bdk.workflow.swadl.v1.activity.stream;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
import com.symphony.bdk.workflow.swadl.v1.activity.Obo;
import com.symphony.bdk.workflow.swadl.v1.activity.OboActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#stream-info-v2">Get stream API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetStream extends OboActivity {
  private String streamId;
}
