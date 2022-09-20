package com.symphony.bdk.workflow.api.v1.dto;

public enum StatusEnum {
  PENDING,
  COMPLETED;

  public static StatusEnum toInstanceStatusEnum(String status) {
    if ("ACTIVE".equalsIgnoreCase(status) || PENDING.name().equalsIgnoreCase(status)) {
      return StatusEnum.PENDING;
    } else if (COMPLETED.name().equalsIgnoreCase(status)) {
      return StatusEnum.COMPLETED;
    } else {
      throw new IllegalArgumentException(String.format("Workflow instance status %s is not known", status));
    }
  }
}
