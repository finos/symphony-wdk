package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
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
  @Nullable private List<String> userIds;
  @Nullable private Map<String, String> keywords;
  @Nullable private String membersCanInvite;
  @Nullable private String discoverable;
  @Nullable private String readOnly;
  @Nullable private String copyProtected;
  @Nullable private String crossPod;
  @Nullable private String viewHistory;
  @Nullable private String multiLateralRoom;
  @Nullable private String subType;

  @JsonProperty("public")
  private String isPublic;

  // to support the usage of variables
  @JsonIgnore
  public List<Long> getUserIdsAsLongs() {
    if (userIds == null) {
      return Collections.emptyList();
    }
    return userIds.stream().map(Long::parseLong).collect(Collectors.toList());
  }

  @JsonIgnore
  public Boolean isPublicAsBool() {
    return Boolean.valueOf(isPublic);
  }

  @JsonIgnore
  @Nullable
  public Boolean isMembersCanInviteAsBool() {
    return Boolean.valueOf(membersCanInvite);
  }

  @JsonIgnore
  @Nullable
  public Boolean isDiscoverableAsBool() {
    return Boolean.valueOf(discoverable);
  }

  @JsonIgnore
  @Nullable
  public Boolean isReadOnlyAsBool() {
    return Boolean.valueOf(readOnly);
  }

  @JsonIgnore
  @Nullable
  public Boolean isCopyProtectedAsBool() {
    return Boolean.valueOf(copyProtected);
  }

  @JsonIgnore
  @Nullable
  public Boolean isCrossPodAsBool() {
    return Boolean.valueOf(crossPod);
  }

  @JsonIgnore
  @Nullable
  public Boolean isViewHistoryAsBool() {
    return Boolean.valueOf(viewHistory);
  }

  @JsonIgnore
  @Nullable
  public Boolean isMultiLateralRoomAsBool() {
    return Boolean.valueOf(multiLateralRoom);
  }

  @Data
  public static class KeywordItem {
    private String key;
    private String value;
  }
}

