package com.symphony.bdk.workflow.versioning.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

@DataJpaTest
public class VersioningWorkflowRepositoryTest {

  @Autowired
  private VersionedWorkflowRepository versionedWorkflowRepository;

  @Test
  void testFindByPath() {
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow().setVersionedWorkflowId("id1", "v1")
        .setSwadl("swadl")
        .setPath("/path/to/swadl/file")
        .setIsToPublish(true);
    versionedWorkflowRepository.save(versionedWorkflow);
    Optional<VersionedWorkflow> workflow = versionedWorkflowRepository.findByPath("/path/to/swadl/file");

    assertThat(workflow.isPresent()).isTrue();
    assertThat(workflow.get()).isEqualTo(versionedWorkflow);
  }

  @Test
  void testFindByPathNotFound() {
    Optional<VersionedWorkflow> workflow = versionedWorkflowRepository.findByPath("/not/found/path");
    assertThat(workflow.isPresent()).isFalse();
  }

  @Test
  void testFindByPathNull() {
    Optional<VersionedWorkflow> workflow = versionedWorkflowRepository.findByPath(null);
    assertThat(workflow.isPresent()).isFalse();
  }

  @Test
  void testFindByVersionedWorkflowIdId() {
    VersionedWorkflow versionedWorkflow1 = new VersionedWorkflow().setVersionedWorkflowId("id1", "v1")
        .setSwadl("swadl")
        .setPath("/path/to/swadl/file/id1/v1")
        .setIsToPublish(true);

    VersionedWorkflow versionedWorkflow2 = new VersionedWorkflow().setVersionedWorkflowId("id1", "v2")
        .setSwadl("swadl")
        .setPath("/path/to/swadl/file/id1/v2")
        .setIsToPublish(true);

    VersionedWorkflow versionedWorkflow3 = new VersionedWorkflow().setVersionedWorkflowId("id2", "v1")
        .setSwadl("swadl")
        .setPath("/path/to/swadl/file/id2/v1")
        .setIsToPublish(true);

    versionedWorkflowRepository.save(versionedWorkflow1);
    versionedWorkflowRepository.save(versionedWorkflow2);
    versionedWorkflowRepository.save(versionedWorkflow3);

    List<VersionedWorkflow> workflows = versionedWorkflowRepository.findByVersionedWorkflowIdId("id1");
    assertThat(workflows).containsExactlyInAnyOrder(versionedWorkflow1, versionedWorkflow2);

    workflows = versionedWorkflowRepository.findByVersionedWorkflowIdId("unfoundId");
    assertThat(workflows).isEmpty();
  }
}
