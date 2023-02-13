package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import com.symphony.bdk.workflow.api.v1.dto.StatusEnum;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowInstQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class WorkflowInstCmdaApiQueryRepository extends CamundaAbstractQueryRepository
        implements WorkflowInstQueryRepository {
  public WorkflowInstCmdaApiQueryRepository(RepositoryService repositoryService, HistoryService historyService,
                                            RuntimeService runtimeService, ObjectConverter objectConverter) {
    super(repositoryService, historyService, runtimeService, objectConverter);
  }

  /**
   * When workflow's first activity starts execution, a row is inserted in historyService's ACT_HI_PROCINST table.
   * This provides us process instances ids.
   * Tested with an activity having its first activity doing a sleep of 3minutes. During the 3 minutes,
   * while the first activity execution is ongoing, this method returns the process instance id.
   */
  @Override
  public List<WorkflowInstanceDomain> findAllById(String id) {
    Map<String, String> processIdVersionTagMap = getProcessIdVersionMap(id, null);
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
            .processDefinitionKey(id)
            .orderByProcessInstanceStartTime()
            .asc()
            .list();

    return objectConverter.convertCollection(instances, processIdVersionTagMap, WorkflowInstanceDomain.class);
  }

  @Override
  public List<WorkflowInstanceDomain> findAllByIdAndVersion(String id, String version) {
    Map<String, String> processIdVersionTagMap = getProcessIdVersionMap(id, version);
    List<HistoricProcessInstance> instances = new ArrayList<>();

    processIdVersionTagMap.keySet().forEach(processDefinitionId -> {
      instances.addAll(historyService.createHistoricProcessInstanceQuery()
              .processDefinitionId(processDefinitionId)
              .orderByProcessInstanceStartTime()
              .asc()
              .list());
    });

    return objectConverter.convertCollection(instances, processIdVersionTagMap, WorkflowInstanceDomain.class);
  }

  private Map<String, String> getProcessIdVersionMap(String id, String version) {
    ProcessDefinitionQuery definitionQuery = repositoryService.createProcessDefinitionQuery().processDefinitionKey(id);
    Optional.ofNullable(version).ifPresent(definitionQuery::versionTag);
    List<ProcessDefinition> definitions = definitionQuery.list();
    return definitions.stream()
            .filter(def -> StringUtils.isNotBlank(def.getVersionTag()))
            .collect(Collectors.toMap(ProcessDefinition::getId, ProcessDefinition::getVersionTag));
  }

  @Override
  public List<WorkflowInstanceDomain> findAllByIdAndStatus(String id, StatusEnum status) {
    return this.findAllByIdAndStatusAndVersion(id, status, null);
  }

  @Override
  public List<WorkflowInstanceDomain> findAllByIdAndStatusAndVersion(String id, StatusEnum status, String version) {
    return query(id, status, version, () ->
            historyService.createHistoricProcessInstanceQuery().processDefinitionKey(id));
  }

  private List<WorkflowInstanceDomain> query(String id, StatusEnum status, String version,
                                             Supplier<HistoricProcessInstanceQuery> supplier) {
    Map<String, String> processIdVersionTagMap = getProcessIdVersionMap(id, version);
    List<HistoricProcessInstance> instances;

    if (StringUtils.isBlank(version)) {
      instances = queryByStatus(status, supplier.get());
    } else {
      instances = queryByStatusAndVersion(processIdVersionTagMap.keySet(), status, supplier.get());
    }

    return objectConverter.convertCollection(instances, processIdVersionTagMap, WorkflowInstanceDomain.class);
  }

  private List<HistoricProcessInstance> queryByStatus(StatusEnum status,
                                                      HistoricProcessInstanceQuery historicProcessInstanceQuery) {
    List<HistoricProcessInstance> instances = new ArrayList<>();
    switch (status) {
      case COMPLETED:
        instances.addAll(historicProcessInstanceQuery.finished()
                .orderByProcessInstanceStartTime()
                .asc()
                .list()
                .stream()
                .filter(instance -> instance.getEndActivityId() != null
                    && instance.getEndActivityId().startsWith("endEvent"))
                .collect(Collectors.toList()));
        break;

      case FAILED:
        instances.addAll(historicProcessInstanceQuery.finished()
                .orderByProcessInstanceStartTime()
                .asc()
                .list()
                .stream()
                .filter(instance -> instance.getEndActivityId() != null
                    && !instance.getEndActivityId().startsWith("endEvent"))
                .collect(Collectors.toList()));
        break;

      case PENDING:
        instances.addAll(historicProcessInstanceQuery.unfinished().orderByProcessInstanceStartTime().asc().list());
        break;

      default:
        break;
    }

    return instances;
  }

  private List<HistoricProcessInstance> queryByStatusAndVersion(
      Set<String> processDefIds, StatusEnum status, HistoricProcessInstanceQuery historicProcessInstanceQuery) {
    List<HistoricProcessInstance> instances = new ArrayList<>();
    switch (status) {
      case COMPLETED:
        processDefIds.forEach(processDefinitionId ->
                instances.addAll(historicProcessInstanceQuery.processDefinitionId(processDefinitionId)
                        .finished()
                        .orderByProcessInstanceStartTime()
                        .asc()
                        .list()
                        .stream()
                        .filter(
                                instance -> instance.getEndActivityId() != null && instance.getEndActivityId()
                                        .startsWith("endEvent"))
                        .collect(Collectors.toList())));
        break;

      case FAILED:
        processDefIds.forEach(processDefinitionId ->
                instances.addAll(historicProcessInstanceQuery.processInstanceId(processDefinitionId)
                        .finished()
                        .orderByProcessInstanceStartTime()
                        .asc()
                        .list()
                        .stream()
                        .filter(
                                instance -> instance.getEndActivityId() != null && !instance.getEndActivityId()
                                        .startsWith("endEvent"))
                        .collect(Collectors.toList())));
        break;

      case PENDING:
        processDefIds.forEach(processDefinitionId ->
                instances.addAll(historicProcessInstanceQuery.processDefinitionId(processDefinitionId)
                        .unfinished()
                        .orderByProcessInstanceStartTime()
                        .asc()
                        .list()));
        break;

      default:
        break;
    }

    return instances;
  }
}
