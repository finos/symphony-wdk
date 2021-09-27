package com.symphony.bdk.workflow.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Simple abstraction to retrieve a file, from the workflows folder when running the bot or from the classpath for
 * tests.
 */
public interface ResourceProvider {

  InputStream getResource(Path relativePath) throws IOException;

  Path saveResource(Path relativePath, byte[] content) throws IOException;
}
