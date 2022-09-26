package com.symphony.bdk.workflow.api.v1.dto;

public enum StatusEnum {
  PENDING,
  COMPLETED,
  FAILED;

  public static StatusEnum toInstanceStatusEnum(String status) {
    if (status == null) {
      return null;
    } else if ("ACTIVE".equalsIgnoreCase(status) || PENDING.name().equalsIgnoreCase(status)) {
      return StatusEnum.PENDING;
    } else if (COMPLETED.name().equalsIgnoreCase(status)) {
      return StatusEnum.COMPLETED;
    } else if (FAILED.name().equalsIgnoreCase(status)) {
      return StatusEnum.FAILED;
    } else {
      throw new IllegalArgumentException(
          String.format("Workflow instance status %s is not known. Allowed values [Completed, Pending, Failed]",
              status));
    }
  }
}
