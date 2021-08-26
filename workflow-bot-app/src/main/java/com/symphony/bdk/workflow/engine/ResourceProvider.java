package com.symphony.bdk.workflow.engine;

import java.io.IOException;
import java.io.InputStream;

/**
 * Simple abstraction to retrieve a file, from the workflows folder when running the bot or from the classpath for
 * tests.
 */
public interface ResourceProvider {

  InputStream getResource(String relativePath) throws IOException;
}
