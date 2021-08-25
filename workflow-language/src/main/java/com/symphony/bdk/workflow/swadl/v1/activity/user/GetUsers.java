package com.symphony.bdk.workflow.swadl.v1.activity.user;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#users-lookup-v3">Search users API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetUsers extends BaseActivity {

  @Nullable
  private List<String> userIds;
  @Nullable
  private List<String> emails;
  @Nullable
  private List<String> usernames;

  @Nullable
  private String local;

  @Nullable
  private String active;

  @Nullable
  @JsonIgnore
  public Boolean getLocalAsBool() {
    return toBoolean(local);
  }

  @Nullable
  @JsonIgnore
  public Boolean getActiveAsBool() {
    return toBoolean(active);
  }
}
