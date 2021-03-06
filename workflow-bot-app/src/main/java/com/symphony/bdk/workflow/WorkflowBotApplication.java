package com.symphony.bdk.workflow;

import lombok.Generated;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Generated // not tested
public class WorkflowBotApplication {

  public static void main(String[] args) {
    disableNashornDeprecationWarning();
    SpringApplication.run(WorkflowBotApplication.class, args);
  }

  private static void disableNashornDeprecationWarning() {
    System.setProperty("nashorn.args", "--no-deprecation-warning");
  }
}
