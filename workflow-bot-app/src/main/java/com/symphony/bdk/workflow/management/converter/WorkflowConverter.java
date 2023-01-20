package com.symphony.bdk.workflow.management.converter;

import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WorkflowConverter implements Converter<String, Workflow> {
  @Override
  public Workflow apply(String content) {
    try {
      return SwadlParser.fromYaml(content);
    } catch (Exception e) {
      log.error("Failed to parse the swadl content", e);
      throw new IllegalArgumentException("SWADL content is not valid");
    }
  }
}
