package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowDomain;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.community.mockito.QueryMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class WorkflowCmdaApiQueryRepositoryTest {
  @Mock RepositoryService repositoryService;
  @Mock ObjectConverter objectConverter;
  @InjectMocks WorkflowCmdaApiQueryRepository queryRepository;

  @Test
  void findAll() {
    // given
    ProcessDefinition instance1 = mock(ProcessDefinition.class);
    ProcessDefinition instance2 = mock(ProcessDefinition.class);
    QueryMocks.mockProcessDefinitionQuery(repositoryService).list(List.of(instance1, instance2));

    WorkflowDomain domain = WorkflowDomain.builder().version(3L).id("wf").name("workflow").build();
    when(objectConverter.convertCollection(anyList(), eq(WorkflowDomain.class))).thenReturn(List.of(domain));

    //when
    List<WorkflowDomain> all = queryRepository.findAll();

    // then
    assertThat(all).hasSize(1);
    assertThat(all.get(0).getId()).isEqualTo("wf");
    assertThat(all.get(0).getName()).isEqualTo("workflow");
    assertThat(all.get(0).getVersion()).isEqualTo(3);
  }
}
