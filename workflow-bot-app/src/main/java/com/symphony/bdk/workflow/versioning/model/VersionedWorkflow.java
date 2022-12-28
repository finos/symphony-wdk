package com.symphony.bdk.workflow.versioning.model;

import lombok.ToString;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;

@Entity
@ToString
//@EqualsAndHashCode
public class VersionedWorkflow {

  @EmbeddedId
  public VersionedWorkflowId versionedWorkflowId;

  @Lob
  public String swadl;
  public String path;
  public boolean isToPublish;

  public VersionedWorkflow() {

  }

  public VersionedWorkflow setSwadl(String swadl) {
    this.swadl = swadl;
    return this;
  }

  public VersionedWorkflow setPath(String path) {
    this.path = path;
    return this;
  }

  public VersionedWorkflow setIsToPublish(boolean isToPublish) {
    this.isToPublish = isToPublish;
    return this;
  }

  /*public VersionedWorkflowId getVersionedWorkflowId() {
    return versionedWorkflowId;
  }*/

  public String getId() {
    return this.versionedWorkflowId.getId();
  }

  public String getVersion() {
    return this.versionedWorkflowId.getVersion();
  }

  public VersionedWorkflow setVersionedWorkflowId(String id, String version) {
    this.versionedWorkflowId = new VersionedWorkflowId(id, version);
    return this;
  }

  public String getSwadl() {
    return swadl;
  }

  public String getPath() {
    return path;
  }

  public boolean isToPublish() {
    return isToPublish;
  }

  /*public void setVersionedWorkflowId(VersionedWorkflowId versionedWorkflowId) {
    this.versionedWorkflowId = versionedWorkflowId;
  }*/
}
