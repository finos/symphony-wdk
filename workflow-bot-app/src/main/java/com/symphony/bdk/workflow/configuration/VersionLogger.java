package com.symphony.bdk.workflow.configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VersionLogger {

  @Value("${version}")
  private String version;

  @PostConstruct
  private void logWdkVersion() {
    if (!StringUtils.isBlank(version)) {
      log.info("Running with WDK version: {}", version);
    }
  }
}
