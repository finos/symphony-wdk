package com.symphony.bdk.workflow;

import com.symphony.bdk.workflow.engine.ResourceProvider;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

public class TestResourcesProvider implements ResourceProvider {

  private final String resourcesFolder;

  public TestResourcesProvider(String resourcesFolder) {
    this.resourcesFolder = resourcesFolder;
  }

  @Override
  public InputStream getResource(Path relativePath) {
    return getClass().getResourceAsStream(relativePath.toString());
  }

  @Override
  public File getResourceFile(Path relativePath) {
    return Path.of(resourcesFolder).resolve(relativePath).toFile();
  }

  @Override
  public Path saveResource(Path relativePath, byte[] content) {
    return Path.of(resourcesFolder).resolve(relativePath);
  }
}
