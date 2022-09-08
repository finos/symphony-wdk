package com.symphony.bdk.workflow.monitoring.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface QueryRepository<T, ID> {
  default Optional<T> findById(ID var1) {
    return Optional.empty();
  }

  default List<T> findAll() {
    return Collections.emptyList();
  }
}
