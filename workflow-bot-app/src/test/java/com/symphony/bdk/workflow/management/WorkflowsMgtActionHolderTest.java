package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.configuration.WorkflowDeployer;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WorkflowsMgtActionHolderTest {

  @Test
  void getInstance() {
    WorkflowsMgtActionHolder holder = new WorkflowsMgtActionHolder(List.of(new WorkflowDeleteAction(mock(
            WorkflowDeployer.class), mock(MonitoringService.class))));
    assertThat(holder.getInstance(WorkflowMgtAction.DELETE)).isInstanceOf(WorkflowDeleteAction.class);
  }
}
