package com.symphony.bdk.workflow.api.v1.dto;

public enum DeploymentExpirationEnum {
  ACTIVE,
  ALL;

  public static DeploymentExpirationEnum toDeploymentExpirationEnum(String type) {
    if (type == null) {
      return null;
    } else if ("ACTIVE".equalsIgnoreCase(type)) {
      return DeploymentExpirationEnum.ACTIVE ;
    } else if ("ALL".equalsIgnoreCase(type)) {
      return DeploymentExpirationEnum.ALL;
    } else {
      throw new IllegalArgumentException(
          String.format("Deployment expiration type %s is not known. Allowed values [ACTIVE, ALL]", type));
    }
  }
}
