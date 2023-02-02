package com.symphony.bdk.workflow.management.converter;

import com.symphony.bdk.workflow.converter.BiConverter;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WorkflowVersionConverter implements BiConverter<String, Long, Workflow> {
  @Override
  public Workflow apply(String content, Long version) {
    try {
      Workflow workflow = SwadlParser.fromYaml(content);
      workflow.setVersion(version);
      return workflow;
    } catch (Exception e) {
      log.error("Failed to parse the swadl content", e);
      throw new IllegalArgumentException("SWADL content is not valid");
    }
  }
}
