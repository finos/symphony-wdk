package com.symphony.bdk.workflow;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class WdkVersionLogger {

  @Value("${wdk.version}")
  private String version;

  @PostConstruct
  private void logWdkVersion() {
    if (!StringUtils.isBlank(version)) {
      log.info("Workflow Developer Kit's version: " + version);
    }
  }
}
