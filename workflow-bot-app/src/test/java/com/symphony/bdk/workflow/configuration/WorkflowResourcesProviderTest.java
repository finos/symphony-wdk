package com.symphony.bdk.workflow.configuration;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowResourcesProviderTest {

  private static final byte[] DATA = new byte[(byte) 123];

  @Test
  void saveResource_getResource(@TempDir Path tempDir) throws IOException {
    WorkflowResourcesProvider provider = new WorkflowResourcesProvider(tempDir.toString());

    Path relativePath = Path.of("test.txt");
    Path path = provider.saveResource(relativePath, DATA);
    assertThat(path).isAbsolute();

    assertThat(IOUtils.toByteArray(provider.getResource(relativePath))).isEqualTo(DATA);
  }

  @Test
  void saveResource_getResourceFile(@TempDir Path tempDir) throws IOException {
    WorkflowResourcesProvider provider = new WorkflowResourcesProvider(tempDir.toString());

    Path relativePath = Path.of("test.txt");
    Path path = provider.saveResource(relativePath, DATA);
    assertThat(path).isAbsolute();

    File resourceFile = provider.getResourceFile(relativePath);
    InputStream inputStream = Files.newInputStream(resourceFile.toPath());
    assertThat(IOUtils.toByteArray(inputStream)).isEqualTo(DATA);
  }
}
