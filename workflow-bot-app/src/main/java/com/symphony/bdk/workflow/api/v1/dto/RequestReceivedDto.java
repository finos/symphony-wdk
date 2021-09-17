package com.symphony.bdk.workflow.api.v1.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RequestReceivedDto {
  private Map<String, Object> args;
}
