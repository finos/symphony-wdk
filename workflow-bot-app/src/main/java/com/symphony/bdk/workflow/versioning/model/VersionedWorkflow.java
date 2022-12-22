package com.symphony.bdk.workflow.versioning.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
@ToString
@EqualsAndHashCode
public class VersionedWorkflow {

  //@EmbeddedId private VersionedWorkflowId versionedWorkflowId;
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String workflowId;

  @Lob
  private String swadl;

  public VersionedWorkflow() {

  }
  public void setSwadl(String swadl) {
    this.swadl = swadl;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public Long getId() {
    return id;
  }

  public String getSwadl() {
    return swadl;
  }
}
