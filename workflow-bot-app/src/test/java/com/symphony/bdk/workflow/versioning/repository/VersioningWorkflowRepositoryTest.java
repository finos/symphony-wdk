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

  private final VersionedWorkflow versionedWorkflow1 = new VersionedWorkflow()
      .setWorkflowId("id1")
      .setVersion("v1")
      .setSwadl("swadl")
      .setPath("/path/to/swadl/file/id1/v1")
      .setIsToPublish(true);

  private final VersionedWorkflow versionedWorkflow2 = new VersionedWorkflow()
      .setWorkflowId("id1")
      .setVersion("v2")
      .setSwadl("swadl")
      .setPath("/path/to/swadl/file/id1/v2")
      .setIsToPublish(true);

  private final VersionedWorkflow versionedWorkflow3 = new VersionedWorkflow()
      .setWorkflowId("id2")
      .setVersion("v1")
      .setSwadl("swadl")
      .setPath("/path/to/swadl/file/id2/v1")
      .setIsToPublish(true);

  @Test
  void testFindByPath() {
    versionedWorkflowRepository.save(versionedWorkflow1);
    Optional<VersionedWorkflow> workflow = versionedWorkflowRepository.findByPath("/path/to/swadl/file/id1/v1");

    assertThat(workflow.isPresent()).isTrue();
    assertThat(workflow.get()).isEqualTo(versionedWorkflow1);
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
  void testFindByWorkflowId() {
    versionedWorkflowRepository.save(versionedWorkflow1);
    versionedWorkflowRepository.save(versionedWorkflow2);
    versionedWorkflowRepository.save(versionedWorkflow3);

    List<VersionedWorkflow> workflows = versionedWorkflowRepository.findByWorkflowId("id1");
    assertThat(workflows).containsExactlyInAnyOrder(versionedWorkflow1, versionedWorkflow2);

    workflows = versionedWorkflowRepository.findByWorkflowId("unfoundId");
    assertThat(workflows).isEmpty();
  }

  @Test
  void testFindByWorkflowIdEmpty() {
    versionedWorkflowRepository.save(versionedWorkflow1);
    versionedWorkflowRepository.save(versionedWorkflow2);
    versionedWorkflowRepository.save(versionedWorkflow3);

    List<VersionedWorkflow> workflows = versionedWorkflowRepository.findByWorkflowId("");
    assertThat(workflows).isEmpty();
  }

  @Test
  void testFindByWorkflowIdNull() {
    versionedWorkflowRepository.save(versionedWorkflow1);
    versionedWorkflowRepository.save(versionedWorkflow2);
    versionedWorkflowRepository.save(versionedWorkflow3);

    List<VersionedWorkflow> workflows = versionedWorkflowRepository.findByWorkflowId(null);
    assertThat(workflows).isEmpty();
  }

  @Test
  void testFindByWorkflowIdAndVersion() {
    versionedWorkflowRepository.save(versionedWorkflow1);
    versionedWorkflowRepository.save(versionedWorkflow2);
    versionedWorkflowRepository.save(versionedWorkflow3);

    Optional<VersionedWorkflow> workflow = versionedWorkflowRepository.findByWorkflowIdAndVersion("id1", "v1");
    assertThat(workflow).isPresent();
    assertThat(workflow.get().getWorkflowId()).isEqualTo("id1");
    assertThat(workflow.get().getVersion()).isEqualTo("v1");

    workflow = versionedWorkflowRepository.findByWorkflowIdAndVersion("unfoundId", "v1");
    assertThat(workflow).isEmpty();

    workflow = versionedWorkflowRepository.findByWorkflowIdAndVersion("id1", "unfoundVersion");
    assertThat(workflow).isEmpty();
  }

  @Test
  void testDeleteAllByWorkflowId() {
    versionedWorkflowRepository.save(versionedWorkflow1);
    versionedWorkflowRepository.save(versionedWorkflow2);
    versionedWorkflowRepository.save(versionedWorkflow3);

    versionedWorkflowRepository.deleteAllByWorkflowId("id1");
    List<VersionedWorkflow> all = versionedWorkflowRepository.findAll();

    assertThat(all).hasSize(1);
    assertThat(all.get(0)).isEqualTo(versionedWorkflow3);
  }
}
