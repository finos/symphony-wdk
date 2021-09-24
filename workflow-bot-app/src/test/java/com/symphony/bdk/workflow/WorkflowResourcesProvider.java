package com.symphony.bdk.workflow;

import com.symphony.bdk.workflow.engine.ResourceProvider;

import java.io.InputStream;

public class WorkflowResourcesProvider implements ResourceProvider {

  private final String resourcesFolder;

  public WorkflowResourcesProvider(String resourcesFolder) {
    this.resourcesFolder = resourcesFolder;
  }

  @Override
  public InputStream getResource(String relativePath) {
    return getClass().getResourceAsStream(relativePath);
  }

  @Override
  public String saveResource(String resourcesFolder, String name, byte[] content) {
    return this.resourcesFolder + name;
  }
}
