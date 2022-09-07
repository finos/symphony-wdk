package com.symphony.bdk.workflow.api.v1.dto;

public enum InstanceStatusEnum {
  PENDING,
  COMPLETED;

  public static InstanceStatusEnum toInstanceStatusEnum(String status) {
    return status.equals("ACTIVE") ? InstanceStatusEnum.PENDING : InstanceStatusEnum.COMPLETED;
  }
}
