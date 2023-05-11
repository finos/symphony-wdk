package com.symphony.bdk.workflow.engine.shared;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "SHARED_STATE_DATA")
@TypeDef(name = "json", typeClass = JsonType.class)
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

  @Type(type = "json")
  @Column(columnDefinition = "json")
  private Map<String, Object> properties = new HashMap<>();

  @Column(name = "LAST_UPDATED", length = 50)
  private Long lastUpdated;

  public SharedData namespace(String namespace) {
    this.setNamespace(namespace);
    return this;
  }

}
