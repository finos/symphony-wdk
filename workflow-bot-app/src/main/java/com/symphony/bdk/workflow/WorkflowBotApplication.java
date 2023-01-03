package com.symphony.bdk.workflow;

import com.symphony.bdk.workflow.logs.LogsStreamingAppender;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.Generated;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
@Generated // not tested
public class WorkflowBotApplication {

  public static void main(String[] args) {
    disableNashornDeprecationWarning();
    ConfigurableApplicationContext context = SpringApplication.run(WorkflowBotApplication.class, args);
    addCustomAppender(context, (LoggerContext) LoggerFactory.getILoggerFactory());
  }

  private static void addCustomAppender(ConfigurableApplicationContext context, LoggerContext loggerContext) {
    LogsStreamingAppender customAppender = context.getBean(LogsStreamingAppender.class);
    Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(customAppender);
  }


  private static void disableNashornDeprecationWarning() {
    System.setProperty("nashorn.args", "--no-deprecation-warning");
  }
}
