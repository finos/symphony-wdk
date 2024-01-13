package com.symphony.bdk.workflow.engine.shared;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "SHARED_STATE_DATA")
@Convert(attributeName = "entityAttrName", converter = JsonType.class)
@Data
public class SharedData {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name = "system-uuid", strategy = "uuid2")
  @Column(name = "ID")
  private String id;

  @NaturalId
  @Column(length = 15)
  private String namespace;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private Map<String, Object> properties = new HashMap<>();

  @Column(name = "LAST_UPDATED", length = 50)
  private Long lastUpdated;

  public SharedData namespace(String namespace) {
    this.setNamespace(namespace);
    return this;
  }
}
