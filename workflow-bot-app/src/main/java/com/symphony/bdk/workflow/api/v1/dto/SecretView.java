package com.symphony.bdk.workflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SecretView {
  @Size(min = 1, max = 15, message = "Secret key value must be between 1 and 15 characters.")
  @Pattern(regexp = "\\S+", message = "Secret key value must not have spaces.")
  private String key;
  @Schema(name = "secret", type = "java.lang.String", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Secret value must not be empty")
  private char[] secret;
}
