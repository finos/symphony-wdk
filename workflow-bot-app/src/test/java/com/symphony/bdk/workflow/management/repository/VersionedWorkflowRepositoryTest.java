package com.symphony.bdk.workflow.management.repository;

import com.symphony.bdk.workflow.management.repository.domain.VersionedWorkflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"wdk.properties.management-token=token"})
public class VersionedWorkflowRepositoryTest {
  @Autowired
  VersionedWorkflowRepository versionedWorkflowRepository;
  VersionedWorkflow versionedWorkflow1;
  VersionedWorkflow versionedWorkflow2;
  VersionedWorkflow versionedWorkflow3;
  VersionedWorkflow versionedWorkflow4;

  static final Long V1 = 1234L;
  static final Long V2 = 1235L;
  static final Long V3 = 1236L;
  static final Long V4 = 1237L;

  @BeforeEach
  void setup() {
    versionedWorkflow1 = new VersionedWorkflow();
    versionedWorkflow1.setWorkflowId("id1");
    versionedWorkflow1.setVersion(V1);
    versionedWorkflow1.setPublished(true);
    versionedWorkflow1.setSwadl("swadl");

    versionedWorkflow2 = new VersionedWorkflow();
    versionedWorkflow2.setWorkflowId("id1");
    versionedWorkflow2.setVersion(V2);
    versionedWorkflow2.setPublished(true);
    versionedWorkflow2.setActive(true);
    versionedWorkflow2.setSwadl("swadl");

    versionedWorkflow3 = new VersionedWorkflow();
    versionedWorkflow3.setWorkflowId("id1");
    versionedWorkflow3.setVersion(V3);
    versionedWorkflow3.setPublished(false);
    versionedWorkflow3.setSwadl("swadl");

    versionedWorkflow4 = new VersionedWorkflow();
    versionedWorkflow4.setWorkflowId("id2");
    versionedWorkflow4.setVersion(V4);
    versionedWorkflow4.setPublished(true);
    versionedWorkflow4.setActive(true);
    versionedWorkflow4.setSwadl("swadl");

    versionedWorkflowRepository.save(versionedWorkflow1);
    versionedWorkflowRepository.save(versionedWorkflow2);
    versionedWorkflowRepository.save(versionedWorkflow3);
    versionedWorkflowRepository.save(versionedWorkflow4);
  }

  @AfterEach
  void cleanup() {
    versionedWorkflowRepository.deleteAll();
  }

  @Test
  void testFindByWorkflowId() {
    List<VersionedWorkflow> workflows = versionedWorkflowRepository.findByWorkflowId("id1");
    assertThat(workflows).containsExactlyInAnyOrder(versionedWorkflow1, versionedWorkflow2, versionedWorkflow3);
  }

  @Test
  void testFindByWorkflowIdEmpty() {
    List<VersionedWorkflow> workflows = versionedWorkflowRepository.findByWorkflowId("");
    assertThat(workflows).isEmpty();
    workflows = versionedWorkflowRepository.findByWorkflowId(null);
    assertThat(workflows).isEmpty();
  }

  @Test
  void testFindByWorkflowIdAndVersion() {
    Optional<VersionedWorkflow> workflow = versionedWorkflowRepository.findByWorkflowIdAndVersion("id1", 1234L);
    assertThat(workflow).isPresent();
    assertThat(workflow.get().getWorkflowId()).isEqualTo("id1");
    assertThat(workflow.get().getVersion()).isEqualTo(V1);

    workflow = versionedWorkflowRepository.findByWorkflowIdAndVersion("unfoundId", V1);
    assertThat(workflow).isEmpty();

    workflow = versionedWorkflowRepository.findByWorkflowIdAndVersion("id1", 1234566L);
    assertThat(workflow).isEmpty();
  }

  @Test
  void testFindByWorkflowIdAndActiveTrue() {
    Optional<VersionedWorkflow> workflow = versionedWorkflowRepository.findByWorkflowIdAndActiveTrue("id1");
    assertThat(workflow).isPresent();
    assertThat(workflow.get().getWorkflowId()).isEqualTo("id1");
    assertThat(workflow.get().getVersion()).isEqualTo(V2);
  }

  @Test
  void findFirstByWorkflowIdOrderByVersionDesc() {
    Optional<VersionedWorkflow> workflow = versionedWorkflowRepository.findByWorkflowIdAndPublishedFalse("id1");
    assertThat(workflow).isPresent();
    assertThat(workflow.get().getWorkflowId()).isEqualTo("id1");
    assertThat(workflow.get().getVersion()).isEqualTo(V3);
  }

  @Test
  void findByActiveTrue() {
    List<VersionedWorkflow> workflows = versionedWorkflowRepository.findByActiveTrue();
    assertThat(workflows).hasSize(2);
  }

  @Test
  void findByWorkflowIdOrderByVersionDesc() {
    Optional<VersionedWorkflow> workflows = versionedWorkflowRepository.findTopByWorkflowIdOrderByVersionDesc("id1");
    assertThat(workflows).isNotEmpty();
    assertThat(workflows.get().getVersion()).isEqualTo(V3);
  }
}
