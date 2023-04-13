package com.symphony.bdk.workflow.engine.executor;

import java.util.Map;

public interface SharedDataStore {
  Map<String, Object> getNamespaceData(String namespace);

  void putNamespaceData(String namespace, String key, Object data);
}
