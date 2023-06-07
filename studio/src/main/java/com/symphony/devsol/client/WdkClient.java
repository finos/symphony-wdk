package com.symphony.devsol.client;

import static java.util.stream.Collectors.toMap;

import com.symphony.bdk.core.auth.jwt.UserClaim;
import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
import com.symphony.bdk.workflow.management.WorkflowManagementService;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WdkClient {
  private final MonitoringService monitoringService;
  private final WorkflowManagementService managementService;
  @Value("${wdk.studio.admins:}")
  private List<Long> admins;

  @GetMapping(value = "v1/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> exportWorkflows(
      @RequestAttribute("user") UserClaim user
  ) {
    if (!admins.contains(user.getId())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    log.info("Starting bulk workflow export..");
    Map<String, String> swadlMap = monitoringService.listAllWorkflows().stream()
        .map(w -> managementService.get(w.getId()).orElse(null))
        .filter(Objects::nonNull)
        .collect(toMap(VersionedWorkflowView::getWorkflowId, VersionedWorkflowView::getSwadl));

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
      for (Map.Entry<String, String> entry : swadlMap.entrySet()) {
        String workflowId = entry.getKey();
        ZipEntry zipEntry = new ZipEntry(workflowId + ".swadl.yaml");
        zipOut.putNextEntry(zipEntry);

        byte[] swadlBytes = swadlMap.get(workflowId).getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = new ByteArrayInputStream(swadlBytes).read(bytes)) >= 0) {
          zipOut.write(bytes, 0, length);
        }
        zipOut.closeEntry();
      }
      zipOut.close();

      log.info("Bulk workflow export complete");

      return ResponseEntity.ok()
          .header("Content-Disposition", "attachment; filename=wdk-export.zip")
          .body(outputStream.toByteArray());
    } catch (IOException e) {
      log.error("Error exporting zip file", e);
      return null;
    }
  }

  @PostMapping("/v1/import")
  public void importWorkflows(
      @RequestParam("file") MultipartFile file,
      @RequestAttribute("user") UserClaim user
  ) {
    if (!admins.contains(user.getId())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    log.info("Starting bulk workflow import..");
    try (ZipInputStream zipIn = new ZipInputStream(file.getInputStream())) {
      ZipEntry entry;

      while ((entry = zipIn.getNextEntry()) != null) {
        if (!entry.getName().endsWith(".swadl.yaml")) {
          continue;
        }
        SwadlView swadlView = SwadlView.builder()
            .createdBy(user.getId())
            .description("Bulk import")
            .swadl(new String(zipIn.readAllBytes(), StandardCharsets.UTF_8))
            .build();
        managementService.deploy(swadlView);
      }
    } catch (IOException e) {
      log.error("Error reading zip file", e);
    }
    log.info("Bulk workflow import complete");
  }
}
