package com.symphony.bdk.workflow.monitoring.service.converter;

import com.symphony.bdk.workflow.api.v1.dto.NodeView;
import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;

import org.springframework.stereotype.Component;

@Component
public class ActivityInstViewConverter implements Converter<ActivityInstanceDomain, NodeView> {

  @Override
  public NodeView apply(ActivityInstanceDomain domain) {
    return NodeView.builder()
        .nodeId(domain.getName())
        .instanceId(domain.getProcInstId())
        .workflowId(domain.getWorkflowId())
        .startDate(domain.getStartDate())
        .endDate(domain.getEndDate())
        .duration(domain.getDuration())
        .outputs(domain.getVariables().getOutputs())
        .build();
  }

}
