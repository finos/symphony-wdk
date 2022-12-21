package com.symphony.bdk.workflow.jpamodel;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import javax.persistence.Embeddable;

@Embeddable
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class VersionedWorkflowId implements Serializable {

  private String id;

  private String version;

  public VersionedWorkflowId(String id, String version) {
    this.id = id;
    this.version = version;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getId() {
    return id;
  }

  public String getVersion() {
    return version;
  }
}
