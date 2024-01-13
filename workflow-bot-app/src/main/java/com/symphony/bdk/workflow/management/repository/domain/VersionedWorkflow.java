package com.symphony.bdk.workflow.management.repository.domain;

import com.symphony.bdk.workflow.management.BigStringCompressor;

import lombok.Data;
import lombok.Generated;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.util.Optional;

@Entity
@Table(name = "VERSIONED_WORKFLOW", uniqueConstraints = @UniqueConstraint(columnNames = {"WORKFLOW_ID", "VERSION"}),
    indexes = {@Index(name = "ID_VERSION_IDX", columnList = "WORKFLOW_ID, VERSION", unique = true),
        @Index(name = "ID_ACTIVE_IDX", columnList = "ACTIVE, WORKFLOW_ID", unique = true)})
@Data
@Generated // not tested
public class VersionedWorkflow {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name = "system-uuid", strategy = "uuid2")
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
  @Convert(converter = BigStringCompressor.class)
  private String swadl;
  @Column(name = "DEPLOY_ID", length = 64)
  private String deploymentId;
  @Column(name = "ACTIVE")
  private Boolean active;
  @Column(name = "CREATED_BY", length = 50)
  private Long createdBy;
  @Column(name = "DESC")
  private String description;

  public Boolean getActive() {
    return Optional.ofNullable(this.active).orElse(false);
  }
}
