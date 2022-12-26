package com.symphony.bdk.workflow.versioning.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import javax.persistence.Embeddable;

@Embeddable
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Data
public class VersionedWorkflowId implements Serializable {

  public String id;

  public String version;

  public VersionedWorkflowId(String id, String version) {
    this.id = id;
    this.version = version;
  }

  public VersionedWorkflowId id(String id) {
    this.id = id;
    return this;
  }

  public VersionedWorkflowId version(String version) {
    this.version = version;
    return this;
  }

  public String getId() {
    return id;
  }

  public String getVersion() {
    return version;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
