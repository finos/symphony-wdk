package com.symphony.bdk.workflow.engine.secret;

import com.symphony.bdk.workflow.exception.DuplicateException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultSecretKeeperTest {
  @Mock
  SecretRepository repository;
  @Mock
  SecretCryptVault vault;
  @InjectMocks
  DefaultSecretKeeper secretKeeper;

  @Test
  void saveSuccessfully() {
    when(repository.findByRef(anyString())).thenReturn(Optional.empty());
    when(vault.encrypt(any())).thenReturn("".getBytes(StandardCharsets.UTF_8));
    when(repository.save(any(SecretDomain.class))).thenReturn(new SecretDomain());
    secretKeeper.save("key", "".getBytes(StandardCharsets.UTF_8));
    verify(repository).save(any(SecretDomain.class));
  }

  @Test
  @DisplayName("Save secret with a key already exists, should fail")
  void saveException() {
    when(repository.findByRef(anyString())).thenReturn(Optional.of(new SecretDomain()));
    Assertions.assertThatThrownBy(() -> secretKeeper.save("key", "".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(DuplicateException.class);
  }

  @Test
  void get() {
    when(repository.findByRef(anyString())).thenReturn(Optional.empty());
    when(vault.encrypt(any())).thenReturn("".getBytes(StandardCharsets.UTF_8));
    when(repository.save(any(SecretDomain.class))).thenReturn(new SecretDomain());
    secretKeeper.save("key", "".getBytes(StandardCharsets.UTF_8));
    verify(repository).save(any(SecretDomain.class));
  }

  @Test
  void remove() {
    doNothing().when(repository).deleteByRef(anyString());
    secretKeeper.remove("key");
    verify(repository).deleteByRef(anyString());
  }
}
