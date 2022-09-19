package com.symphony.bdk.workflow.converter;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default object converter implementation.
 *
 * <p>Subscribe a list of available converters using their class type as compose key.
 */
@Component
public class DefaultObjectConverter implements ObjectConverter {
  private final Map<ConverterKey, Converter> converterMap = new HashMap<>();

  public DefaultObjectConverter(List<Converter> converters) {
    converters.forEach(
        converter -> converterMap.put(new ConverterKey(converter.getSourceClass(), converter.getTargetClass()),
            converter));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T convert(Object source, Class<T> targetClass) {
    if (Objects.isNull(source)) {
      return null;
    }

    Converter converter = converterMap.get(new ConverterKey(source.getClass(), targetClass));
    Class clz = source.getClass();
    while (converter == null && clz != null) {
      clz = clz.getSuperclass();
      converter = converterMap.get(new ConverterKey(clz, targetClass));
    }

    if (Objects.isNull(converter)) {
      throw new IllegalArgumentException("Cannot find converter for " + targetClass);
    }
    return (T) converter.apply(source);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T convert(Object source, Class<?> sourceClass, Class<T> targetClass) {
    if (Objects.isNull(source)) {
      return null;
    }
    if (!sourceClass.isAssignableFrom(source.getClass())) {
      throw new IllegalArgumentException("Cannot find converter for the given source type " + sourceClass);
    }
    Converter converter = converterMap.get(new ConverterKey(source.getClass(), targetClass));
    if (Objects.isNull(converter)) {
      throw new IllegalArgumentException("Cannot find converter for " + targetClass);
    }
    return (T) converter.apply(source);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> List<T> convertCollection(List<?> source, Class<T> targetClass) {
    if (Objects.isNull(source) || source.isEmpty()) {
      return Collections.emptyList();
    }

    Converter converter = converterMap.get(new ConverterKey(source.get(0).getClass(), targetClass));
    if (Objects.isNull(converter)) {
      throw new IllegalArgumentException("Cannot find converter for " + targetClass);
    }

    List<T> collection = converter.applyCollection(source);
    return collection.isEmpty() ? Collections.emptyList() : collection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> List<T> convertCollection(List<?> source, Class<?> sourceClass, Class<T> targetClass) {
    if (Objects.isNull(source) || source.isEmpty()) {
      return Collections.emptyList();
    }

    if (!sourceClass.isAssignableFrom(source.get(0).getClass())) {
      throw new IllegalArgumentException("Cannot find converter for the given source type " + sourceClass);
    }

    Converter converter = converterMap.get(new ConverterKey(sourceClass, targetClass));
    if (Objects.isNull(converter)) {
      throw new IllegalArgumentException("Cannot find converter for " + targetClass);
    }

    List<T> collection = converter.applyCollection(source);
    return collection.isEmpty() ? Collections.emptyList() : collection;
  }

  /**
   * Compose-key for converter registry map key.
   */
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class ConverterKey {

    private final Class<?> sourceClass;

    private final Class<?> targetClass;
  }
}
