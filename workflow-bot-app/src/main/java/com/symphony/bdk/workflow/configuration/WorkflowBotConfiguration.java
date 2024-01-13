package com.symphony.bdk.workflow.configuration;

import com.symphony.bdk.ext.group.SymphonyGroupBdkExtension;
import com.symphony.bdk.workflow.engine.ResourceProvider;
import com.symphony.bdk.workflow.engine.secret.SecretCryptVault;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@Configuration
@Getter
@Profile("!test")
public class WorkflowBotConfiguration {

  @Value("${wdk.workflows.path:./workflows}")
  private String workflowsFolderPath;

  @Value("${wdk.properties.monitoring-token:}")
  private String monitoringToken;

  @Value("${wdk.properties.management-token:}")
  private String managementToken;

  @Bean("workflowResourcesProvider")
  public ResourceProvider workflowResourcesProvider() {
    // the folder is used both to load workflows and local resources
    return new WorkflowResourcesProvider(this.workflowsFolderPath);
  }

  @Bean
  public SymphonyGroupBdkExtension groupExtension() {
    return new SymphonyGroupBdkExtension();
  }

  @Bean
  public SecretCryptVault cryptJpaVault(@Value("${wdk.encrypt.passphrase}") String pwd)
      throws InvalidKeySpecException, NoSuchAlgorithmException {
    if (pwd.length() < 16) {
      throw new IllegalArgumentException("The encryption passphrase length must have at least 16 characters.");
    }
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    KeySpec spec = new PBEKeySpec(pwd.toCharArray(), pwd.substring(0, 9).getBytes(StandardCharsets.UTF_8), 65536, 256);
    SecretKey tmp = factory.generateSecret(spec);
    return new SecretCryptVault().with256BitAesGcmNoPaddingAnd64BitSaltKey(0, tmp.getEncoded())
        .withDefaultKeyVersion(0);
  }
}
