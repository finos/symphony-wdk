package com.symphony.bdk.workflow.versioning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
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
      new VersionedWorkflow().setWorkflowId("id").setVersion("v1").setPath("path/to/swadl/file").setSwadl("swadl");

  @Test
  void testSaveWithPublish() {
    when(versionedWorkflowRepository.save(any(VersionedWorkflow.class))).thenReturn(null);
    versionedWorkflow.setIsToPublish(false);
    versioningService.save("id", "v1", "swadl", "path/to/swadl/file", "", false);

    ArgumentCaptor<VersionedWorkflow> captor = ArgumentCaptor.forClass(VersionedWorkflow.class);

    verify(versionedWorkflowRepository).save(captor.capture());
    assertThat(captor.getValue().getWorkflowId()).isEqualTo("id");
    assertThat(captor.getValue().getVersion()).isEqualTo("v1");
    assertThat(captor.getValue().getSwadl()).isEqualTo("swadl");
    assertThat(captor.getValue().getPath()).isEqualTo("path/to/swadl/file");
    assertThat(captor.getValue().isToPublish()).isFalse();
  }

  @Test
  void testSaveWithoutPublish() {
    when(versionedWorkflowRepository.save(any(VersionedWorkflow.class))).thenReturn(null);
    versioningService.save("id", "v1", "swadl", "path/to/swadl/file", "");

    ArgumentCaptor<VersionedWorkflow> captor = ArgumentCaptor.forClass(VersionedWorkflow.class);

    verify(versionedWorkflowRepository).save(captor.capture());
    assertThat(captor.getValue().getWorkflowId()).isEqualTo("id");
    assertThat(captor.getValue().getVersion()).isEqualTo("v1");
    assertThat(captor.getValue().getSwadl()).isEqualTo("swadl");
    assertThat(captor.getValue().getPath()).isEqualTo("path/to/swadl/file");
    assertThat(captor.getValue().isToPublish()).isTrue();
  }

  @Test
  void testDelete() {
    doNothing().when(versionedWorkflowRepository).deleteAllByWorkflowId(any(String.class));
    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    versioningService.delete("id");

    verify(versionedWorkflowRepository).deleteAllByWorkflowId(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).isEqualTo("id");
  }

  @Test
  void testDeleteEmptyId() {
    doNothing().when(versionedWorkflowRepository).deleteAllByWorkflowId(any(String.class));
    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    versioningService.delete("");

    verify(versionedWorkflowRepository).deleteAllByWorkflowId(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).isEqualTo("");
  }

  @Test
  void testFindWithIdAndVersion() {
    when(versionedWorkflowRepository.findByWorkflowIdAndVersion(any(), any())).thenReturn(
        Optional.of(versionedWorkflow));
    ArgumentCaptor<String> workflowIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> versionCaptor = ArgumentCaptor.forClass(String.class);
    versioningService.findByWorkflowIdAndVersion("id", "v1");

    verify(versionedWorkflowRepository).findByWorkflowIdAndVersion(workflowIdCaptor.capture(), versionCaptor.capture());
    assertThat(workflowIdCaptor.getValue()).isEqualTo("id");
    assertThat(versionCaptor.getValue()).isEqualTo("v1");
  }

  @Test
  void testFindWithId() {
    when(versionedWorkflowRepository.findByWorkflowId(any(String.class))).thenReturn(
        Collections.singletonList(versionedWorkflow));
    versioningService.findByWorkflowId("id");

    verify(versionedWorkflowRepository).findByWorkflowId(eq("id"));
  }

  @Test
  void testFindWithSwadlPath() {
    when(versionedWorkflowRepository.findByPath(any())).thenReturn(Optional.of(versionedWorkflow));
    versioningService.findByPath(Path.of("path/to/swadl/file"));

    verify(versionedWorkflowRepository).findByPath(eq("path/to/swadl/file"));
  }
}
