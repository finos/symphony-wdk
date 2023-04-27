package com.symphony.bdk.workflow.engine.secret;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecretRepository extends JpaRepository<SecretDomain, String> {
  Optional<SecretDomain> findByRef(String ref);

  void deleteByRef(String ref);
}
