package com.symphony.bdk.workflow.configuration;

import com.symphony.bdk.workflow.engine.ResourceProvider;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class WorkflowResourcesProvider implements ResourceProvider {

  private final String resourcesFolder;

  public WorkflowResourcesProvider(@Value("${workflows.folder}") String resourcesFolder) {
    this.resourcesFolder = resourcesFolder;
  }

  @Override
  public InputStream getResource(String relativePath) throws IOException {
    return new FileInputStream(StringUtils.appendIfMissing(this.resourcesFolder, "/") + relativePath);
  }

  @Override
  public String saveResource(String name, byte[] content) {
    String path = StringUtils.appendIfMissing(this.resourcesFolder, "/") + name;
    File file = this.createFile(path);
    return this.writeContent(file, content);
  }

  private File createFile(String name) {
    try {
      File file = new File(name);
      if (file.createNewFile()) {
        log.info("File {} has been created", name);
      } else {
        log.info("File {} already exist", name);
      }

      return file;
    } catch (IOException exception) {
      log.debug("File {} creation failed", name, exception);
      throw new RuntimeException(exception);
    }
  }

  private String writeContent(File file, byte[] content) {
    String filePath = file.getPath();
    try (FileOutputStream outputStream = new FileOutputStream(file)) {
      log.info("File {} content has been written", filePath);
      outputStream.write(content);
      return filePath;
    } catch (Exception exception) {
      log.debug("File {} writing failed", filePath, exception);
      throw new RuntimeException(exception);
    }
  }
}
