package com.symphony.bdk.workflow.versioning.model;

import lombok.EqualsAndHashCode;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import javax.persistence.Embeddable;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@Getter
@Generated // not tested
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
}
