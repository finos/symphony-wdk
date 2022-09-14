package com.symphony.bdk.workflow.api.v1.dto;

public enum StatusEnum {
  PENDING,
  COMPLETED;

  public static StatusEnum toInstanceStatusEnum(String status) {
    return "ACTIVE".equals(status) ? StatusEnum.PENDING : StatusEnum.COMPLETED;
  }
}
