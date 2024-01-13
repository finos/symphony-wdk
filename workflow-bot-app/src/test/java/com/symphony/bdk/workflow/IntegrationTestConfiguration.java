package com.symphony.bdk.workflow;

import com.symphony.bdk.workflow.configuration.WorkflowBotConfiguration;
import com.symphony.bdk.workflow.engine.ResourceProvider;
import com.symphony.bdk.workflow.engine.secret.SecretCryptVault;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
@Slf4j
@Profile("test")
public class IntegrationTestConfiguration {
  @Bean("workflowResourcesProvider")
  public ResourceProvider workflowResourcesProvider() {
    return new TestResourcesProvider(Paths.get("dummy").toString());
  }

  @Bean(name = "workflowBotConfiguration")
  public WorkflowBotConfiguration workflowBotConfiguration() {
    WorkflowBotConfiguration workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    when(workflowBotConfiguration.getMonitoringToken()).thenReturn("MONITORING_TOKEN_VALUE");
    when(workflowBotConfiguration.getManagementToken()).thenReturn("MANAGEMENT_TOKEN_VALUE");
    when(workflowBotConfiguration.getWorkflowsFolderPath()).thenReturn("./");
    return workflowBotConfiguration;
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
