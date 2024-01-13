package com.symphony.bdk.workflow.engine.secret;

import org.springframework.scheduling.annotation.Scheduled;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.function.Function;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecretCryptVault {
  private static final String DEFAULT_CIPHER = "AES/GCM/NoPadding";
  private static final String DEFAULT_ALGORITHM = "AES";
  private static final int DEFAULT_SALT_LENGTH = 8;
  private static final int ALGORITHM_NONCE_SIZE = 12;
  private static final int ALGORITHM_TAG_SIZE = 128;
  private SecureRandom secureRandom = new SecureRandom();
  private final CryptVersion[] cryptVersions = new CryptVersion[256];
  private int defaultVersion = -1;

  /**
   * AES simply pads to 128 bits
   */
  private static final Function<Integer, Integer> AESLengthCalculator = i -> (i | 0xf) + 1;

  /**
   * Helper method for the preferred config of the vault. Should not be changed, except for better algorithm
   *
   * @param version version of key - keys should be rotated every now and then
   * @param secret  key for the algorithm
   */
  public SecretCryptVault with256BitAesGcmNoPaddingAnd64BitSaltKey(int version, byte[] secret) {
    if (secret.length != 32) {
      throw new IllegalArgumentException("invalid AES key size; should be 256 bits!");
    }
    Key key = new SecretKeySpec(secret, DEFAULT_ALGORITHM);
    CryptVersion cryptVersion = new CryptVersion(DEFAULT_SALT_LENGTH, DEFAULT_CIPHER, key, AESLengthCalculator);
    return withKey(version, cryptVersion);
  }

  public SecretCryptVault withKey(int version, CryptVersion cryptVersion) {
    if (version >= 0 && version <= 255) {
      if (this.cryptVersions[version] != null) {
        throw new IllegalArgumentException("version " + version + " is already defined");
      } else {
        this.cryptVersions[version] = cryptVersion;
        if (version > this.defaultVersion) {
          this.defaultVersion = version;
        }
        return this;
      }
    } else {
      throw new IllegalArgumentException("version must be a byte");
    }
  }

  public byte[] encrypt(byte[] plaintext) {
    return this.encrypt(this.defaultVersion, plaintext);
  }

  public byte[] encrypt(int version, byte[] plaintext) {
    CryptVersion cryptVersion = this.cryptVersion(version);
    byte versionByte = toSignedByte(version);
    try {
      // Generate a 96-bit nonce using a CSPRNG.
      byte[] nonce = new byte[ALGORITHM_NONCE_SIZE];
      secureRandom.nextBytes(nonce);

      // Create the cipher instance and initialize.
      Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER);
      cipher.init(Cipher.ENCRYPT_MODE, cryptVersion.key, new GCMParameterSpec(ALGORITHM_TAG_SIZE, nonce));

      // Encrypt and prepend nonce.
      byte[] ciphertext = cipher.doFinal(plaintext);
      byte[] ciphertextAndNonce = new byte[nonce.length + ciphertext.length + 1];
      ciphertextAndNonce[0] = versionByte;
      System.arraycopy(nonce, 0, ciphertextAndNonce, 1, nonce.length);
      System.arraycopy(ciphertext, 0, ciphertextAndNonce, nonce.length + 1, ciphertext.length);

      return ciphertextAndNonce;
    } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException
             | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
      throw new CryptOperationException("JCE exception caught while encrypting with version " + version, e);
    }
  }

  public byte[] decrypt(byte[] ciphertextAndNonce) {
    int version = fromSignedByte(ciphertextAndNonce[0]);
    CryptVersion cryptVersion = cryptVersion(version);
    try {
      // Retrieve the nonce and ciphertext.
      byte[] nonce = new byte[ALGORITHM_NONCE_SIZE];
      byte[] ciphertext = new byte[ciphertextAndNonce.length - ALGORITHM_NONCE_SIZE - 1];
      System.arraycopy(ciphertextAndNonce, 1, nonce, 0, nonce.length);
      System.arraycopy(ciphertextAndNonce, nonce.length + 1, ciphertext, 0, ciphertext.length);

      // Create the cipher instance and initialize.
      Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER);
      cipher.init(Cipher.DECRYPT_MODE, cryptVersion.key, new GCMParameterSpec(ALGORITHM_TAG_SIZE, nonce));

      // Decrypt and return result.
      return cipher.doFinal(ciphertext);
    } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException
             | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
      throw new CryptOperationException("JCE exception caught while encrypting with version " + version, e);
    }
  }

  private CryptVersion cryptVersion(int version) {
    try {
      CryptVersion result = cryptVersions[version];
      if (result == null) {
        throw new IllegalArgumentException("version " + version + " undefined");
      }
      return result;
    } catch (IndexOutOfBoundsException e) {
      if (version < 0) {
        throw new IllegalStateException("encryption keys are not initialized");
      }
      throw new IllegalArgumentException("version must be a byte (0-255)");
    }
  }

  public SecretCryptVault withDefaultKeyVersion(int defaultVersion) {
    if (defaultVersion >= 0 && defaultVersion <= 255) {
      if (this.cryptVersions[defaultVersion] == null) {
        throw new IllegalArgumentException("version " + defaultVersion + " is undefined");
      } else {
        this.defaultVersion = defaultVersion;
        return this;
      }
    } else {
      throw new IllegalArgumentException("version must be a byte");
    }
  }

  @Scheduled(initialDelay = 3_600_000, fixedDelay = 3_600_000)
  public void reInitSecureRandomHourly() {
    // performance regards: Depending on the implementation, the generateSeed and nextBytes methods may block as
    // entropy is being gathered, for example, if they need to read from /dev/random on various Unix-like operating
    // systems
    secureRandom = new SecureRandom();
  }

  public static byte toSignedByte(int val) {
    return (byte) (val + Byte.MIN_VALUE);
  }

  public static int fromSignedByte(byte val) {
    return (int) val - Byte.MIN_VALUE;
  }

  public static class CryptVersion {
    public final int saltLength;
    public final String cipher;
    public final Key key;
    public final Function<Integer, Integer> encryptedLength;

    public CryptVersion(int saltLength, String cipher, Key key, Function<Integer, Integer> encryptedLength) {
      this.saltLength = saltLength;
      this.cipher = cipher;
      this.key = key;
      this.encryptedLength = encryptedLength;
    }
  }
}
