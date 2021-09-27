package com.symphony.bdk.workflow.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

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
}
