package com.symphony.bdk.workflow.engine;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceProvider {

  InputStream getResource(String relativePath) throws IOException;
}
