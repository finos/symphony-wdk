package com.symphony.bdk.workflow.jpamodel;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@ToString
@EqualsAndHashCode
public class VersionedWorkflow {

  //@EmbeddedId private VersionedWorkflowId versionedWorkflowId;
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long workflowId;
  private String swadl;

  public VersionedWorkflow() {

  }

  public VersionedWorkflow(Long  workflowId, String swadl) {
    this.workflowId = workflowId;
    this.swadl = swadl;
  }


  public void setSwadl(String swadl) {
    this.swadl = swadl;
  }

  public void setWorkflowId(Long workflowId) {
    this.workflowId = workflowId;
  }

  public Long getWorkflowId() {
    return workflowId;
  }

  public String getSwadl() {
    return swadl;
  }
}
