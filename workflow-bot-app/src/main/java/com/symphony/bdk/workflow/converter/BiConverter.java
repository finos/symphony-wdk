package com.symphony.bdk.workflow.converter;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public interface BiConverter<S, K, T> extends BiFunction<S, K, T> {
  /**
   * The source type for which this is going to convert.
   *
   * @return the source class type
   */
  @SuppressWarnings("unchecked")
  default Class<S> getSourceClass() {
    return (Class<S>) ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
  }

  /**
   * The target type to which this is going to convert.
   *
   * @return the target class type
   */
  @SuppressWarnings("unchecked")
  default Class<T> getTargetClass() {
    return (Class<T>) ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[2];
  }

  /**
   * Convert the source collection objects to a list of target type objects.
   *
   * @param source a collection of source objects.
   * @return a list of target objects.
   */
  default List<T> applyCollection(final List<S> source, final K object) {
    return source.stream().map(s -> this.apply(s, object)).filter(e -> !Objects.isNull(e)).collect(Collectors.toList());
  }
}
