package com.symphony.bdk.workflow.converter;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Default object converter implementation.
 *
 * <p>Subscribe a list of available converters using their class type as compose key.
 */
@Component
public class DefaultObjectConverter implements ObjectConverter {
  private final Map<ConverterKey, Converter> converterMap = new HashMap<>();
  private final Map<ConverterKey, BiConverter> biConverterMap = new HashMap<>();

  public DefaultObjectConverter(List<Converter> converters, Optional<List<BiConverter>> optionalBiConverters) {
    converters.forEach(
        converter -> converterMap.put(new ConverterKey(converter.getSourceClass(), converter.getTargetClass()),
            converter));
    optionalBiConverters.orElse(List.of())
        .forEach(
            converter -> biConverterMap.put(new ConverterKey(converter.getSourceClass(), converter.getTargetClass()),
                converter));
  }

  private <K> K getConverter(Object source, Class<?> sourceClass, Class<?> targetClass,
      Function<ConverterKey, K> func) {
    if (Objects.isNull(source)) {
      return null;
    }
    if (!sourceClass.isAssignableFrom(source.getClass())) {
      throw new IllegalArgumentException("Cannot find converter for the given source type " + sourceClass);
    }

    K converter = func.apply(new ConverterKey(sourceClass, targetClass));
    Class clz = sourceClass;
    while (converter == null && clz != null) {
      clz = clz.getSuperclass();
      converter = func.apply(new ConverterKey(clz, targetClass));
    }

    if (Objects.isNull(converter)) {
      throw new IllegalArgumentException("Cannot find converter for " + targetClass);
    }
    return converter;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T convert(Object source, Class<T> targetClass) {
    Converter converter =
        getConverter(source, Optional.ofNullable(source).map(Object::getClass).orElse(null), targetClass,
            converterMap::get);
    return (T) converter.apply(source);
  }

  @Override
  public <T> T convert(Object source, Object object, Class<T> targetClass) {
    BiConverter converter =
        getConverter(source, Optional.ofNullable(source).map(Object::getClass).orElse(null), targetClass,
            biConverterMap::get);
    return (T) converter.apply(source, object);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T convert(Object source, Class<?> sourceClass, Class<T> targetClass) {
    Converter converter = getConverter(source, sourceClass, targetClass, converterMap::get);
    return (T) converter.apply(source);
  }

  @Override
  public <T> T convert(Object source, Object object, Class<?> sourceClass, Class<T> targetClass) {
    BiConverter converter = getConverter(source, sourceClass, targetClass, biConverterMap::get);
    return (T) converter.apply(source, object);
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

    return converter.applyCollection(source);
  }

  @Override
  public <T> List<T> convertCollection(List<?> source, Object object, Class<T> targetClass) {
    if (Objects.isNull(source) || source.isEmpty()) {
      return Collections.emptyList();
    }

    BiConverter converter = biConverterMap.get(new ConverterKey(source.get(0).getClass(), targetClass));
    if (Objects.isNull(converter)) {
      throw new IllegalArgumentException("Cannot find converter for " + targetClass);
    }

    return converter.applyCollection(source, object);
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

    return converter.applyCollection(source);
  }

  @Override
  public <T> List<T> convertCollection(List<?> source, Object object, Class<?> sourceClass, Class<T> targetClass) {
    if (Objects.isNull(source) || source.isEmpty()) {
      return Collections.emptyList();
    }

    if (!sourceClass.isAssignableFrom(source.get(0).getClass())) {
      throw new IllegalArgumentException("Cannot find converter for the given source type " + sourceClass);
    }

    BiConverter converter = biConverterMap.get(new ConverterKey(sourceClass, targetClass));
    if (Objects.isNull(converter)) {
      throw new IllegalArgumentException("Cannot find converter for " + targetClass);
    }

    return converter.applyCollection(source, object);
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
