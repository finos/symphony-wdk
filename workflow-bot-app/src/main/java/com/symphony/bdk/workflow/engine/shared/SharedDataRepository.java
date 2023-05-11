package com.symphony.bdk.workflow.engine.shared;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedDataRepository extends JpaRepository<SharedData, String> {
  Optional<SharedData> findByNamespace(String namespace);
}
