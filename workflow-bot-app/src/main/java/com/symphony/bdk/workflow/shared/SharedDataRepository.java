package com.symphony.bdk.workflow.shared;

import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
public interface SharedDataRepository extends JpaRepository<SharedData, String> {
  Optional<SharedData> findByNamespace(String namespace);
}
