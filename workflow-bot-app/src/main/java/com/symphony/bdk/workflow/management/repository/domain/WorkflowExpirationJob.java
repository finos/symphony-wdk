package com.symphony.bdk.workflow.management.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "EXPIRATION_JOB")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Generated // not tested
public class WorkflowExpirationJob {
  @Id
  @Column(name = "VERSIONED_WORKFLOW_ID", nullable = false, length = 100)
  private String id;

  @Column(name = "WORKFLOW_ID", nullable = false, length = 100)
  private String workflowId;

  @Column(name = "DEPLOYMENT_ID", nullable = false)
  private String deploymentId;

  @Column(name = "EXPIRATION_DATE", nullable = false)
  private Instant expirationDate;
}
