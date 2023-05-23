package com.symphony.bdk.workflow.api.v1.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApiModel
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SecretView {
  private String key;
  @ApiModelProperty(name = "secret", dataType = "java.lang.String", required = true)
  private char[] secret;
}
