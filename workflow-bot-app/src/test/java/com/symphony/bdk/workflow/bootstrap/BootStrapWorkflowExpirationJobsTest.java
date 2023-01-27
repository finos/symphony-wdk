package com.symphony.bdk.workflow.bootstrap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.expiration.WorkflowExpirationPlanner;
import com.symphony.bdk.workflow.versioning.model.WorkflowExpirationJob;
import com.symphony.bdk.workflow.versioning.repository.WorkflowExpirationJobRepository;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class BootStrapWorkflowExpirationJobsTest {

  @Test
  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  void setupWorkflowExpirationJobsTest() {
    final WorkflowExpirationJobRepository mockedRepository = mock(WorkflowExpirationJobRepository.class);
    final WorkflowExpirationPlanner mockedPlanner = mock(WorkflowExpirationPlanner.class);

    BootStrapWorkflowExpirationJobs bootStrap = new BootStrapWorkflowExpirationJobs(mockedRepository, mockedPlanner);

    WorkflowExpirationJob workflowExpirationJob1 = new WorkflowExpirationJob();
    WorkflowExpirationJob workflowExpirationJob2 = new WorkflowExpirationJob();

    workflowExpirationJob1.setId("id1");
    workflowExpirationJob1.setWorkflowId("workflowId1");
    workflowExpirationJob1.setDeploymentId("deployment1");
    workflowExpirationJob1.setExpirationDate(null);

    workflowExpirationJob1.setId("id2");
    workflowExpirationJob1.setWorkflowId("workflowId2");
    workflowExpirationJob1.setDeploymentId("deployment2");
    workflowExpirationJob1.setExpirationDate(null);

    when(mockedRepository.findAll()).thenReturn(Arrays.asList(workflowExpirationJob1, workflowExpirationJob2));
    doNothing().when(mockedPlanner).planExpiration(any());

    bootStrap.setupWorkflowExpirationJobs();

    verify(mockedRepository).findAll();
    verify(mockedPlanner, times(2)).planExpiration(any());
  }
}
