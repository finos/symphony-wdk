package com.symphony.bdk.workflow.swadl.v1.activity.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Two different activities have been created to separate normal/system users and provide better completion/validation
 * of SWADL. In the Java model we kept a single model since we are calling the same API in the executor.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CreateSystemUser extends CreateUser {

  public CreateSystemUser() {
    setType("SYSTEM");
  }
}
