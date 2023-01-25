package com.symphony.bdk.workflow.api.v1.dto;

public enum InstanceStatusEnum {
  PENDING,
  COMPLETED,
  FAILED;

  public static InstanceStatusEnum toInstanceStatusEnum(String status) {
    if (status == null) {
      return null;
    } else if ("ACTIVE".equalsIgnoreCase(status) || PENDING.name().equalsIgnoreCase(status)) {
      return InstanceStatusEnum.PENDING;
    } else if (COMPLETED.name().equalsIgnoreCase(status)) {
      return InstanceStatusEnum.COMPLETED;
    } else if (FAILED.name().equalsIgnoreCase(status)) {
      return InstanceStatusEnum.FAILED;
    } else {
      throw new IllegalArgumentException(
          String.format("Workflow instance status %s is not known. Allowed values [Completed, Pending, Failed]",
              status));
    }
  }
}
