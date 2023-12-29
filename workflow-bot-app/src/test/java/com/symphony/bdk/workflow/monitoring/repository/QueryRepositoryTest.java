package com.symphony.bdk.workflow.monitoring.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class QueryRepositoryTest {

  @Test
  void findById() {
    QueryRepository<String, String> repository = new TestQueryRepository();
    then(repository.findById("")).isEmpty();
  }

  @Test
  void findAll() {
    QueryRepository<String, String> repository = new TestQueryRepository();
    then(repository.findAll()).isEmpty();
  }

  private static class TestQueryRepository implements QueryRepository<String, String> {}
}
