package com.symphony.bdk.workflow.converter;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Object/Entities converter.
 *
 * <p>Implementation must provide the source type and target type to define the converter. The
 * default method will convert the same type for the collection.
 */
public interface Converter<S, T> extends Function<S, T> {

  /**
   * The source type for which this is going to convert.
   *
   * @return the source class type
   */
  default Class<S> getSourceClass() {
    return (Class<S>) ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
  }

  /**
   * The target type to which this is going to convert.
   *
   * @return the target class type
   */
  default Class<T> getTargetClass() {
    return (Class<T>) ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[1];
  }

  /**
   * Convert the source collection objects to a list of target type objects.
   *
   * @param source a collection of source objects.
   * @return a list of target objects.
   */
  default List<T> applyCollection(final List<S> source) {
    return source.stream().map(this).filter(e -> !Objects.isNull(e)).collect(Collectors.toList());
  }
}
