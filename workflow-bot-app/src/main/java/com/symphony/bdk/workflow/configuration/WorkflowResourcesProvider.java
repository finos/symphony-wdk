package com.symphony.bdk.workflow.configuration;

import com.symphony.bdk.workflow.engine.ResourceProvider;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

@Slf4j
public class WorkflowResourcesProvider implements ResourceProvider {

  private final String resourcesFolder;

  public WorkflowResourcesProvider(@Value("${workflows.folder}") String resourcesFolder) {
    this.resourcesFolder = resourcesFolder;
  }

  @Override
  public InputStream getResource(Path relativePath) throws IOException {
    return new FileInputStream(Path.of(resourcesFolder).resolve(relativePath).toFile());
  }

  @Override
  public Path saveResource(Path relativePath, byte[] content) throws IOException {
    Path absolutePath = Path.of(resourcesFolder).resolve(relativePath);
    FileUtils.writeByteArrayToFile(absolutePath.toFile(), content);
    return absolutePath;
  }

}
