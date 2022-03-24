package com.symphony.bdk.workflow.swadl.v1.activity.group;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference/insertgroup">Add group API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CreateGroup extends BaseActivity {
  private String type = "SDL";
  private Owner owner;
  private String name;
  @Nullable private String subType;
  @Nullable private String referrer;
  private List<GroupMember> members = List.of();

  @Nullable private Profile profile;
  @Nullable private VisibilityRestriction visibilityRestriction;
  @Nullable private ImplicitConnection implicitConnection;
  @Nullable private InteractionTransfer interactionTransfer;


  @Data
  public static class GroupMember {
    private Long userId;
    private Integer tenantId;
  }


  @Data
  public static class Owner {
    private Long id;
    private String type;
  }


  @Data
  public static class Profile {
    @Nullable private String displayName;
    @Nullable private String companyName;
    @Nullable private String email;
    @Nullable private String mobile;
    @Nullable private Job job;

    @Nullable private List<String> industries;
    @Nullable private List<String> assetClasses;
    @Nullable private Set<String> marketCoverages;
    @Nullable private Set<String> responsibilities;
    @Nullable private Set<String> functions;
    @Nullable private Set<String> instruments;
  }


  @Data
  public static class Job {
    @Nullable private String title;
    @Nullable private String role;
    @Nullable private String department;
    @Nullable private String division;
    @Nullable private String phone;
    @Nullable private String city;
  }


  @Data
  public static class VisibilityRestriction {
    @Nullable private List<Integer> tenantIds;
    @Nullable private List<Long> userIds;
  }


  @Data
  public static class ImplicitConnection {
    @Nullable private List<Integer> tenantIds;
    @Nullable private List<Long> userIds;
  }


  @Data
  public static class InteractionTransfer {
    @Nullable private List<Integer> tenantIds;
    @Nullable private List<Long> userIds;
  }
}
