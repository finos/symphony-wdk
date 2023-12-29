package com.symphony.bdk.workflow.engine.secret;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import static org.assertj.core.api.Assertions.assertThat;

class SecretCryptVaultTest {

  @Test
  void testEncryptAndDecrypt() throws Exception {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    KeySpec spec = new PBEKeySpec("password".toCharArray(), "salt".getBytes(StandardCharsets.UTF_8), 65536, 256);
    SecretKey tmp = factory.generateSecret(spec);

    SecretCryptVault vault =
        new SecretCryptVault().with256BitAesGcmNoPaddingAnd64BitSaltKey(0, tmp.getEncoded())
            .withDefaultKeyVersion(0);

    String toEncrypt = "this is to be encrypted";

    byte[] encrypt = vault.encrypt(toEncrypt.getBytes(StandardCharsets.UTF_8));

    String decrypted = new String(vault.decrypt(encrypt), StandardCharsets.UTF_8);
    assertThat(toEncrypt).isEqualTo(decrypted);
  }

}
