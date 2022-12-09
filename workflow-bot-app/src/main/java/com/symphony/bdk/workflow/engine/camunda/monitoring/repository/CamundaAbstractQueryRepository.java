package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import com.symphony.bdk.workflow.converter.ObjectConverter;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;

@RequiredArgsConstructor
public abstract class CamundaAbstractQueryRepository {
  protected final RepositoryService repositoryService;
  protected final HistoryService historyService;
  protected final RuntimeService runtimeService;
  protected final ManagementService managementService;
  protected final ObjectConverter objectConverter;
}
