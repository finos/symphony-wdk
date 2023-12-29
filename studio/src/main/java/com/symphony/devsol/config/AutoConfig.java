package com.symphony.devsol.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Slf4j
@EnableCaching
@Configuration
@ComponentScan(basePackages = "com.symphony.devsol")
@EnableWebMvc
public class AutoConfig {
  @PostConstruct
  public void init() {
    log.info("Starting WDK Studio");
  }
}
