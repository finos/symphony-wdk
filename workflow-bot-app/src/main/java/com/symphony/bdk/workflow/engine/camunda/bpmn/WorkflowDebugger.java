package com.symphony.bdk.workflow.engine.camunda.bpmn;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

@Slf4j
public final class WorkflowDebugger {

  static void generateDebugFiles(String workflowName, BpmnModelInstance instance) {
    // avoid polluting current folder in dev, keep it working for deployment/Docker
    File outputFolder = new File("./build");
    if (!outputFolder.exists() || !outputFolder.isDirectory()) {
      outputFolder = new File(".");
    }

    File bpmnFile = new File(outputFolder, workflowName + ".bpmn");
    Bpmn.writeModelToFile(bpmnFile, instance);
    log.debug("BPMN file generated to {}", bpmnFile);
    try {
      // uses https://github.com/bpmn-io/bpmn-to-image
      File pngFile = new File(outputFolder, workflowName + ".png");
      Runtime.getRuntime().exec(
          String.format("bpmn-to-image --title %s-%s %s:%s",
              workflowName, Instant.now(), bpmnFile, pngFile));
      log.debug("BPMN, image outputFolder generated to {}", pngFile);
    } catch (IOException ioException) {
      log.warn("Failed to convert BPMN to image, make sure it is installed (npm install -g bpmn-to-image)",
          ioException);
    }
  }
}
