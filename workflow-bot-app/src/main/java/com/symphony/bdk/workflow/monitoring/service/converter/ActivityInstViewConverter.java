package com.symphony.bdk.workflow.monitoring.service.converter;

import com.symphony.bdk.workflow.api.v1.dto.ActivityInstanceView;
import com.symphony.bdk.workflow.api.v1.dto.VariableView;
import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;

import org.springframework.stereotype.Component;

@Component
public class ActivityInstViewConverter implements Converter<ActivityInstanceDomain, ActivityInstanceView> {

  @Override
  public ActivityInstanceView apply(ActivityInstanceDomain domain) {
    if (domain.getType().equals("serviceTask") || domain.getType().equals("scriptTask")) {
      return ActivityInstanceView.builder()
          .activityId(domain.getName())
          .instanceId(domain.getProcInstId())
          .workflowId(domain.getWorkflowId())
          .startDate(domain.getStartDate())
          .endDate(domain.getEndDate())
          .duration(domain.getDuration())
          .outputs(domain.getVariables().getOutputs())
          .build();
    }
    return null;
  }

}
