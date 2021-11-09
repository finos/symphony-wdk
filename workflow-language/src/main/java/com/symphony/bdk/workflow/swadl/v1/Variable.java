package com.symphony.bdk.workflow.swadl.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Variable<?> variable = (Variable<?>) o;
    return Objects.equals(resolvedValue, variable.resolvedValue) && Objects.equals(variableReference,
        variable.variableReference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resolvedValue, variableReference);
  }
}
