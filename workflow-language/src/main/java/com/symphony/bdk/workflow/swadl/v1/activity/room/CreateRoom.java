package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.Variable;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#create-room-v3">Create room API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CreateRoom extends BaseActivity {
  @Nullable private String roomName;
  @Nullable private String roomDescription;
  private Variable<List<Variable<Number>>> userIds = Variable.nullValue();
  @Nullable private Variable<Map<String, String>> keywords;
  private Variable<Boolean> membersCanInvite = Variable.nullValue();
  private Variable<Boolean> discoverable = Variable.nullValue();
  private Variable<Boolean> readOnly = Variable.nullValue();
  private Variable<Boolean> copyProtected = Variable.nullValue();
  private Variable<Boolean> crossPod = Variable.nullValue();
  private Variable<Boolean> viewHistory = Variable.nullValue();
  private Variable<Boolean> multiLateralRoom = Variable.nullValue();
  @Nullable private String subType;

  @JsonProperty("public")
  private Variable<Boolean> isPublic = Variable.nullValue();

  @JsonIgnore
  @Nullable
  public List<Long> getUserIdsAsLongs() {
    if (userIds.get() == null) {
      return null;
    }
    return userIds.get().stream()
        .map(Variable::get)
        .map(Number::longValue)
        .collect(Collectors.toList());
  }

  @Data
  public static class KeywordItem {
    private String key;
    private String value;
  }
}

