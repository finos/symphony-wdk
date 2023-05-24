package com.symphony.bdk.workflow.engine.secret;

import com.symphony.bdk.workflow.engine.executor.SecretKeeper;
import com.symphony.bdk.workflow.exception.DuplicateException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class DefaultSecretKeeper implements SecretKeeper {
  private final SecretRepository repository;

  private final SecretCryptVault vault;

  public DefaultSecretKeeper(SecretRepository repository, SecretCryptVault vault) {
    this.repository = repository;
    this.vault = vault;
  }

  @Override
  public void save(String key, byte[] secret) {
    Optional<SecretDomain> existing = repository.findByRef(key);
    if (existing.isPresent()) {
      log.error("Secret reference key exists already.");
      throw new DuplicateException("Secret reference key exists already.");
    }
    repository.save(new SecretDomain(key, Base64.getEncoder().encodeToString(vault.encrypt(secret))));
  }

  @Override
  public String get(String key) {
    String secret = repository.findByRef(key).orElse(new SecretDomain()).getSecret();
    return secret == null ? null
        : new String(vault.decrypt(Base64.getDecoder().decode(secret)), StandardCharsets.UTF_8);
  }

  @Override
  public void remove(String key) {
    repository.deleteByRef(key);
  }

  @Override
  public List<SecretMetadata> getSecretsMetadata() {
    return repository.findAll()
        .stream()
        .map(domain -> new SecretMetadata(domain.getRef(), Instant.ofEpochMilli(domain.getCreatedAt())))
        .collect(
            Collectors.toList());
  }
}
