package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import com.symphony.bdk.workflow.api.v1.dto.StatusEnum;
import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class WorkflowInstanceDomainConverter
    implements Converter<HistoricProcessInstanceEntity, WorkflowInstanceDomain> {

  @Override
  public WorkflowInstanceDomain apply(HistoricProcessInstanceEntity historicProcessInstance) {
    return WorkflowInstanceDomain.builder()
        .id(historicProcessInstance.getId())
        .name(historicProcessInstance.getProcessDefinitionKey())
        .instanceId(historicProcessInstance.getProcessInstanceId())
        .version(historicProcessInstance.getProcessDefinitionVersion())
        .startDate(historicProcessInstance.getStartTime().toInstant())
        .endDate(historicProcessInstance.getEndTime() == null ? null : historicProcessInstance.getEndTime().toInstant())
        .status(resolveStatus(historicProcessInstance.getState(), historicProcessInstance.getEndActivityId()))
        .duration(historicProcessInstance.getDurationInMillis() == null ? null
            : Duration.ofMillis(historicProcessInstance.getDurationInMillis()))
        .build();
  }

  private String resolveStatus(String historicProcessInstanceState, String historicProcessInstanceEndActivityId) {
    if ("ACTIVE".equalsIgnoreCase(historicProcessInstanceState) || StatusEnum.PENDING.name()
        .equalsIgnoreCase(historicProcessInstanceState)) {
      return StatusEnum.PENDING.name();
    }

    if (StatusEnum.COMPLETED.name().equalsIgnoreCase(historicProcessInstanceState)) {
      if (StringUtils.isNotBlank(historicProcessInstanceEndActivityId)
          && historicProcessInstanceEndActivityId.startsWith("endEvent")) {
        return StatusEnum.COMPLETED.name();
      } else {
        return StatusEnum.FAILED.name();
      }
    }

    return null;
  }
}
