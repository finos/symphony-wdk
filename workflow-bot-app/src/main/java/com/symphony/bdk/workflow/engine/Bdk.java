package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.core.service.message.util.PresentationMLParser;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

/**
 * Utilities for EL evaluation by Camunda.
 */
@Service
public class Bdk {

  public String escape(String s) {
    return s.replace("\"", "\\\"");
  }

  @SneakyThrows
  public String text(String presentationMl) {
    return PresentationMLParser.getTextContent(presentationMl);
  }

}
