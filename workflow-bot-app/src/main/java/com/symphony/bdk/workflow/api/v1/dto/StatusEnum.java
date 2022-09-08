package com.symphony.bdk.workflow.api.v1.dto;

public enum StatusEnum {
  PENDING,
  COMPLETED;

  public static StatusEnum toInstanceStatusEnum(String status) {
    return status.equals("ACTIVE") ? StatusEnum.PENDING : StatusEnum.COMPLETED;
  }
}
