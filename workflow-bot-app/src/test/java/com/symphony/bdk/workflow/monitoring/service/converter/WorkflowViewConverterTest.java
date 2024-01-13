package com.symphony.bdk.workflow.monitoring.service.converter;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowDomain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowViewConverterTest {

  @Test
  void apply() {
    // given
    WorkflowDomain domain = WorkflowDomain.builder().id("id").version(1234L).name("workflow").build();

    //when
    WorkflowViewConverter converter = new WorkflowViewConverter();
    WorkflowView view = converter.apply(domain);

    // then
    assertThat(view.getId()).isEqualTo("workflow");
    assertThat(view.getVersion()).isEqualTo(1234L);
  }
}
