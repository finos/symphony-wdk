package com.symphony.bdk.workflow.versioning.model;

import lombok.Data;
import lombok.Generated;
import org.hibernate.annotations.GenericGenerator;

import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(name = "VERSIONED_WORKFLOW", uniqueConstraints = @UniqueConstraint(columnNames = {"WORKFLOW_ID", "VERSION"}),
    indexes = {@Index(name = "ID_VERSION_IDX", columnList = "WORKFLOW_ID, VERSION", unique = true),
        @Index(name = "ID_ACTIVE_IDX", columnList = "WORKFLOW_ID, ACTIVE", unique = true)})
@Data
@Generated // not tested
public class VersionedWorkflow {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name = "system-uuid", strategy = "uuid")
  @Column(name = "ID")
  private String id;
  @Column(name = "WORKFLOW_ID", nullable = false, length = 100)
  private String workflowId;
  @Column(name = "VERSION", nullable = false)
  private Long version;
  @Column(name = "PUBLISHED", nullable = false)
  private Boolean published;
  @Version
  @Column(name = "ETAG")
  private Long etag;
  @Lob
  @Column(name = "SWADL", length = Integer.MAX_VALUE, nullable = false)
  private String swadl;
  @Column(name = "DEPLOY_ID", length = 64)
  private String deploymentId;
  @Column(name = "ACTIVE")
  private Boolean active;
  @Column(name = "UPDATED_BY", length = 50)
  private String userId;
  @Column(name = "DESC")
  private String description;

  public Boolean getActive() {
    return Optional.ofNullable(this.active).orElse(false);
  }
}
