package com.symphony.bdk.workflow.engine.secret;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "SECRET", indexes = {@Index(name = "SECRET_REF_IDX", columnList = "SECRET_REF", unique = true)})
@NoArgsConstructor
@Data
public class SecretDomain {

  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name = "system-uuid", strategy = "uuid2")
  @Column(name = "ID")
  private String id;

  @Column(name = "SECRET_REF", length = 15)
  private String ref;

  @Column(name = "ENCRYPTED")
  private String secret;

  @Column(name = "CREATED_AT", nullable = false, updatable = false)
  private Long createdAt;

  public SecretDomain(String ref, String secret) {
    this.ref = ref;
    this.secret = secret;
  }

  @PrePersist
  protected void onCreate() {
    createdAt = Instant.now().toEpochMilli();
  }
}
