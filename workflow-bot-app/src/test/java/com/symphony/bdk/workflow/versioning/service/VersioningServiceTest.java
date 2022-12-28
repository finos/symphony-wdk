package com.symphony.bdk.workflow.versioning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflowId;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

@DataJpaTest
public class VersioningServiceTest {

  @Mock
  VersionedWorkflowRepository versionedWorkflowRepository;

  @InjectMocks
  VersioningService versioningService;

  private final VersionedWorkflow versionedWorkflow =
      new VersionedWorkflow().setVersionedWorkflowId("id", "v1").setPath("path/to/swadl/file").setSwadl("swadl");

  @Test
  void testSaveWithPublish() {
    when(versionedWorkflowRepository.save(any(VersionedWorkflow.class))).thenReturn(null);
    versionedWorkflow.setIsToPublish(false);
    versioningService.save("id", "v1", "swadl", "path/to/swadl/file", false);

    verify(versionedWorkflowRepository).save(versionedWorkflow);
  }

  @Test
  void testSaveWithoutPublish() {
    when(versionedWorkflowRepository.save(any(VersionedWorkflow.class))).thenReturn(null);
    versioningService.save("id", "v1", "swadl", "path/to/swadl/file");

    verify(versionedWorkflowRepository).save(versionedWorkflow);
  }

  @Test
  void testDelete() {
    doNothing().when(versionedWorkflowRepository).delete(any(VersionedWorkflow.class));
    ArgumentCaptor<VersionedWorkflow> argumentCaptor = ArgumentCaptor.forClass(VersionedWorkflow.class);
    versioningService.delete("id", "v1");

    verify(versionedWorkflowRepository).delete(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getVersionedWorkflowId().getId()).isEqualTo("id");
    assertThat(argumentCaptor.getValue().getVersionedWorkflowId().getVersion()).isEqualTo("v1");
  }

  @Test
  void testDeleteEmptyVersion() {
    doNothing().when(versionedWorkflowRepository).delete(any(VersionedWorkflow.class));
    ArgumentCaptor<VersionedWorkflow> argumentCaptor = ArgumentCaptor.forClass(VersionedWorkflow.class);
    versioningService.delete("id", "");

    verify(versionedWorkflowRepository).delete(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getVersionedWorkflowId().getId()).isEqualTo("id");
    assertThat(argumentCaptor.getValue().getVersionedWorkflowId().getVersion()).isEqualTo("");
  }

  @Test
  void testFindWithIdAndVersion() {
    when(versionedWorkflowRepository.findById(any(VersionedWorkflowId.class))).thenReturn(
        Optional.of(versionedWorkflow));
    ArgumentCaptor<VersionedWorkflowId> argumentCaptor = ArgumentCaptor.forClass(VersionedWorkflowId.class);
    versioningService.find("id", "v1");

    verify(versionedWorkflowRepository).findById(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getId()).isEqualTo("id");
    assertThat(argumentCaptor.getValue().getVersion()).isEqualTo("v1");
  }

  @Test
  void testFindWithId() {
    when(versionedWorkflowRepository.findByVersionedWorkflowIdId(any(String.class))).thenReturn(
        Collections.singletonList(versionedWorkflow));
    versioningService.find("id");

    verify(versionedWorkflowRepository).findByVersionedWorkflowIdId(eq("id"));
  }

  @Test
  void testFindWithSwadlPath() {
    when(versionedWorkflowRepository.findByPath(any())).thenReturn(Optional.of(versionedWorkflow));
    versioningService.find(Path.of("path/to/swadl/file"));

    verify(versionedWorkflowRepository).findByPath(eq("path/to/swadl/file"));
  }
}
