package com.symphony.bdk.workflow.engine.shared;

import com.symphony.bdk.workflow.engine.executor.SharedDataStore;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class DefaultSharedDataStore implements SharedDataStore {
  private final SharedDataRepository repository;

  @Override
  public Map<String, Object> getNamespaceData(String namespace) {
    return repository.findByNamespace(namespace).orElse(new SharedData()).getProperties();
  }

  @Override
  public void putNamespaceData(String namespace, String key, Object data) {
    SharedData sharedData = repository.findByNamespace(namespace).orElse(new SharedData().namespace(namespace));
    sharedData.getProperties().put(key, data);
    sharedData.setLastUpdated(Instant.now().toEpochMilli());
    repository.save(sharedData);
  }
}
