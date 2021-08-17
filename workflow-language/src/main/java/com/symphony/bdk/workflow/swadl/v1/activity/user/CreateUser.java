package com.symphony.bdk.workflow.swadl.v1.activity.user;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#create-user-v2">Create user API</a>
 * @see <a href="https://developers.symphony.com/restapi/reference#update-user-status">Update user API</a>
 * @see <a href="https://developers.symphony.com/restapi/reference#update-features">Update user features API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CreateUser extends BaseActivity {

  private String type = "NORMAL";
  private String email;
  private String username;
  private String firstname;
  private String lastname;
  private String displayName;
  private String recommendedLanguage;

  @Nullable
  private Contact contact;
  @Nullable
  private Business business;

  @Nullable
  private List<String> roles;

  @Nullable
  private Map<String, Boolean> entitlements;

  @Nullable
  private String status;

  // TODO password?


  @Data
  public static class Contact {
    private String workPhoneNumber;
    private String mobilePhoneNumber;
    private String twoFactorAuthNumber;
    private String smsNumber;
  }


  @Data
  public static class Business {
    private String companyName;
    private String department;
    private String division;
    private String title;
    private String location;
    private String jobFunction;
    private List<String> assetClasses;
    private List<String> industries;
    private List<String> functions;
    private List<String> marketCoverages;
    private List<String> responsibilities;
    private List<String> instruments;
  }
}
