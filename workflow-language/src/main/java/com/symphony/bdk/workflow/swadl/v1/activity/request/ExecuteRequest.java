package com.symphony.bdk.workflow.swadl.v1.activity.request;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.Map;

/**
 * A "curl" like activity to execute HTTP requests
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class ExecuteRequest extends BaseActivity {
  private String url;
  private String method;
  private Map<String, Object> body = Collections.emptyMap();
  private Map<String, String> headers = Collections.emptyMap();
}
