package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import com.symphony.bdk.workflow.api.v1.dto.StatusEnum;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;

import java.time.Duration;

public abstract class AbstractInstanceDomainConverter {

  protected WorkflowInstanceDomain.WorkflowInstanceDomainBuilder instanceCommonBuilder(
      HistoricProcessInstanceEntity hisProcInstance) {
    return WorkflowInstanceDomain.builder()
        .id(hisProcInstance.getId())
        .name(hisProcInstance.getProcessDefinitionKey())
        .instanceId(hisProcInstance.getProcessInstanceId())
        .startDate(hisProcInstance.getStartTime().toInstant())
        .endDate(hisProcInstance.getEndTime() == null ? null : hisProcInstance.getEndTime().toInstant())
        .status(resolveStatus(hisProcInstance.getState(), hisProcInstance.getEndActivityId()))
        .duration(hisProcInstance.getDurationInMillis() == null ? null
            : Duration.ofMillis(hisProcInstance.getDurationInMillis()));
  }

  protected String resolveStatus(String hisProcInstanceState, String hisProcInstanceEndActivityId) {
    if ("ACTIVE".equalsIgnoreCase(hisProcInstanceState) || StatusEnum.PENDING.name()
        .equalsIgnoreCase(hisProcInstanceState)) {
      return StatusEnum.PENDING.name();
    }

    if (StatusEnum.COMPLETED.name().equalsIgnoreCase(hisProcInstanceState)) {
      if (StringUtils.isNotBlank(hisProcInstanceEndActivityId)
          && hisProcInstanceEndActivityId.startsWith("endEvent")) {
        return StatusEnum.COMPLETED.name();
      } else {
        return StatusEnum.FAILED.name();
      }
    }

    return null;
  }
}
