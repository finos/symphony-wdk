package com.symphony.bdk.workflow.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.configuration.WorkflowDeployer;

import org.junit.jupiter.api.Test;

import java.util.List;

class WorkflowsMgtActionHolderTest {

  @Test
  void getInstance() {
    WorkflowsMgtActionHolder holder = new WorkflowsMgtActionHolder(List.of(new WorkflowDeleteAction(mock(
        WorkflowDeployer.class))));
    assertThat(holder.getInstance(WorkflowMgtAction.DELETE)).isInstanceOf(WorkflowDeleteAction.class);
  }
}
