package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;

import org.camunda.bpm.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ActivityDomainConverter implements Converter<HistoricActivityInstanceEntity, ActivityInstanceDomain> {

  @Override
  public ActivityInstanceDomain apply(HistoricActivityInstanceEntity historicActivityInstance) {
    return ActivityInstanceDomain.builder()
        .id(historicActivityInstance.getId())
        .name(historicActivityInstance.getActivityId())
        .procInstId(historicActivityInstance.getProcessInstanceId())
        .workflowId(historicActivityInstance.getProcessDefinitionKey())
        .type(historicActivityInstance.getActivityType())
        .startDate(historicActivityInstance.getStartTime().toInstant())
        .endDate(
            historicActivityInstance.getEndTime() == null ? null : historicActivityInstance.getEndTime().toInstant())
        .duration(historicActivityInstance.getDurationInMillis() == null ? null
            : Duration.ofMillis(historicActivityInstance.getDurationInMillis()))
        .build();
  }
}
