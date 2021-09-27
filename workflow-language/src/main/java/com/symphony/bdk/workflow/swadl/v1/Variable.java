package com.symphony.bdk.workflow.swadl.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Variable<T> {
  public static final String RESOLVED_VALUE_FIELD = "val";
  public static final String VARIABLE_REFERENCE_FIELD = "var";

  @JsonProperty(Variable.RESOLVED_VALUE_FIELD)
  private T resolvedValue;

  @SuppressWarnings("unused")
  @JsonProperty(VARIABLE_REFERENCE_FIELD)
  private String variableReference;

  public static <T> Variable<T> nullValue() {
    return new Variable<>(null);
  }

  public static <T> Variable<T> value(T value) {
    return new Variable<>(value);
  }

  public Variable(String variableReference) {
    this.variableReference = variableReference;
  }

  public Variable(T resolvedValue) {
    this.resolvedValue = resolvedValue;
  }

  @JsonIgnore
  public T get() {
    return resolvedValue;
  }

  @JsonIgnore
  public Integer getInt() {
    if (resolvedValue instanceof Number) {
      return ((Number) resolvedValue).intValue();
    } else {
      return null;
    }
  }

}
