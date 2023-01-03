package com.symphony.bdk.workflow.versioning.model;

import lombok.Generated;
import lombok.Getter;
import org.hibernate.annotations.GenericGenerator;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workflowId", "version"}),
    indexes = @Index(name = "versionedWorkflowIndex", columnList = "workflowId, version", unique = true))
@Getter
@Generated // not tested
public class VersionedWorkflow {
  @Id @org.springframework.data.annotation.Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name = "system-uuid", strategy = "uuid")
  @Column(name = "id")
  private String id;

  private String workflowId;

  private String version;

  private String deploymentId;

  @Lob
  @Column(length = Integer.MAX_VALUE)
  private String swadl;
  private String path;
  private boolean isToPublish;

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

  public VersionedWorkflow setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  public VersionedWorkflow setVersion(String version) {
    this.version = version;
    return this;
  }

  public VersionedWorkflow setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
    return this;
  }

  public VersionedWorkflow setId(String id) {
    this.id = id;
    return this;
  }

  public String getId() {
    return this.id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VersionedWorkflow that = (VersionedWorkflow) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, workflowId, version, deploymentId, swadl, path, isToPublish);
  }
}
