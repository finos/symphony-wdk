package com.symphony.bdk.workflow.versioning.model;

import lombok.Generated;
import lombok.Getter;
import org.hibernate.Hibernate;

import java.util.Objects;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;

@Entity
@Getter
@Generated // not tested
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

  public VersionedWorkflow setVersionedWorkflowId(String id, String version) {
    this.versionedWorkflowId = new VersionedWorkflowId(id, version);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    VersionedWorkflow that = (VersionedWorkflow) o;
    return versionedWorkflowId != null && Objects.equals(versionedWorkflowId, that.versionedWorkflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(versionedWorkflowId);
  }
}
