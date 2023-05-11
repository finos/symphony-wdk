package com.symphony.bdk.workflow.engine.executor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

public interface SecretKeeper {
  void save(String key, byte[] secret);

  String get(String key);

  void remove(String key);

  List<SecretMetadata> getSecretsMetadata();

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SecretMetadata {
    private String secretKey;
    private Instant createdAt;
  }
}
