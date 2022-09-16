package com.symphony.bdk.workflow.monitoring.service.converter;

import static org.assertj.core.api.BDDAssertions.then;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowDomain;

import org.junit.jupiter.api.Test;

class WorkflowViewConverterTest {

  @Test
  void apply() {
    // given
    WorkflowDomain domain = WorkflowDomain.builder().id("id").version(2).name("workflow").build();

    //when
    WorkflowViewConverter converter = new WorkflowViewConverter();
    WorkflowView view = converter.apply(domain);

    // then
    then(view.getId()).isEqualTo("workflow");
    then(view.getVersion()).isEqualTo("2");
  }
}
