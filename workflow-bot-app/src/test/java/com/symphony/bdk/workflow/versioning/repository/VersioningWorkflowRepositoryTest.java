package com.symphony.bdk.workflow.versioning.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.workflow.configuration.WorkflowDataSourceConfiguration;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

@DataJpaTest(properties = {"wdk.properties.management-token=token"})
@Import({WorkflowDataSourceConfiguration.class})
public class VersioningWorkflowRepositoryTest {
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
    Optional<VersionedWorkflow> workflow = versionedWorkflowRepository.findFirstByWorkflowIdOrderByVersionDesc("id1");
    assertThat(workflow).isPresent();
    assertThat(workflow.get().getWorkflowId()).isEqualTo("id1");
    assertThat(workflow.get().getVersion()).isEqualTo(V3);
  }

  @Test
  void testDeleteAllByWorkflowId() {
    boolean exist = versionedWorkflowRepository.existsByWorkflowId("id1");
    assertThat(exist).isTrue();

    versionedWorkflowRepository.deleteByWorkflowId("id1");
    List<VersionedWorkflow> all = versionedWorkflowRepository.findAll();

    assertThat(all).hasSize(1);
    assertThat(all.get(0)).isEqualTo(versionedWorkflow4);
  }
}
