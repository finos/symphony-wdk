package com.symphony.bdk.workflow.versioning.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;

@Entity
@ToString
@EqualsAndHashCode
public class VersionedWorkflow {

  @EmbeddedId
  public VersionedWorkflowId versionedWorkflowId;
  /*@Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;*/

  @Lob
  public String swadl;

  public VersionedWorkflow() {

  }
  public VersionedWorkflow setSwadl(String swadl) {
    this.swadl = swadl;
    return this;
  }

  public VersionedWorkflowId getVersionedWorkflowId() {
    return versionedWorkflowId;
  }

  public VersionedWorkflow setVersionedWorkflowId(String id, String version) {
    this.versionedWorkflowId = new VersionedWorkflowId(id, version);
    return this;
  }

  public String getSwadl() {
    return swadl;
  }

  public void setVersionedWorkflowId(VersionedWorkflowId versionedWorkflowId) {
    this.versionedWorkflowId = versionedWorkflowId;
  }
}
