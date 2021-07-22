package org.acme.workflow;

import com.symphony.bdk.workflow.lang.swadl.activity.BaseActivity;

public class MyActivity extends BaseActivity {
  private String myParameter;

  public String getMyParameter() {
    return myParameter;
  }

  public void setMyParameter(String myParameter) {
    this.myParameter = myParameter;
  }
}
