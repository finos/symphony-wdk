package com.symphony.bdk.workflow.engine.camunda.bpmn;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;

/**
 * Be aware that enabling debug logs will log the entire workflow's content. Sensitive data might be there!
 */
@Slf4j
public final class WorkflowDebugger {

  static void generateDebugFiles(String workflowName, BpmnModelInstance instance) {
    // avoid polluting current folder in dev, keep it working for deployment/Docker
    File outputFolder = new File("./build");
    if (!outputFolder.exists() || !outputFolder.isDirectory()) {
      outputFolder = new File(".");
    }

    File bpmnFile = generateBpmn(workflowName, instance, outputFolder);
    logFileAsDataUri("text/xml", bpmnFile);

    generateImage(workflowName, outputFolder, bpmnFile);
  }

  private static File generateBpmn(String workflowName, BpmnModelInstance instance, File outputFolder) {
    File bpmnFile = new File(outputFolder, workflowName + ".bpmn");
    Bpmn.writeModelToFile(bpmnFile, instance);
    log.debug("BPMN file generated to {}", bpmnFile);
    return bpmnFile;
  }

  private static void logFileAsDataUri(String mediaType, File file) {
    try {
      byte[] content = FileUtils.readFileToByteArray(file);
      String base64Content = Base64.getEncoder().encodeToString(content);
      log.debug("File {} content: data:{};base64,{}", file.getName(), mediaType, base64Content);
      if (mediaType.equals("text/xml")) {
        log.debug(
            "Direct visualization on {}data:text/xml;base64,{}",
            "https://cdn.staticaly.com/gh/bpmn-io/bpmn-js-examples/master/url-viewer/index.html?url=",
            base64Content);
      }
    } catch (IOException e) {
      log.warn("Failed to read back BPMN file", e);
    }
  }

  private static File generateImage(String workflowName, File outputFolder, File bpmnFile) {
    try {
      // uses https://github.com/bpmn-io/bpmn-to-image
      File pngFile = new File(outputFolder, workflowName + ".png");
      Runtime.getRuntime().exec(
          String.format("bpmn-to-image --title %s-%s %s:%s",
              workflowName, Instant.now(), bpmnFile, pngFile));
      log.debug("BPMN, image outputFolder generated to {}", pngFile);
      return pngFile;
    } catch (IOException ioException) {
      log.warn("Failed to convert BPMN to image, make sure it is installed (npm install -g bpmn-to-image)",
          ioException);
    }
    return outputFolder;
  }

  private WorkflowDebugger() {
    // utility class
  }
}
