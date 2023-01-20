package com.symphony.bdk.workflow.converter;

import java.util.List;

/**
 * Object converter, convert a given object to a target object if their converter is registered.
 */
public interface ObjectConverter {

  /**
   * Convert the given object to target object in the given type.
   *
   * @param source object to be converted
   * @param targetClass target object class type
   * @param <T>    target object type
   * @return converted target object
   */
  <T> T convert(Object source, Class<T> targetClass);

  /**
   * Convert the given object to target object in the given type.
   *
   * @param source object to be converted
   * @param object object helps to convert the source object
   * @param targetClass target object class type
   * @param <T>    target object type
   * @return converted target object
   */
  <T> T convert(Object source, Object object, Class<T> targetClass);

  /**
   * Convert the given object to target object in the given type.
   *
   * @param source object to be converted
   * @param sourceClass source object type (mainly used to reference to the subscribed type)
   * @param targetClass target object class type
   * @param <T>    target object type
   * @return converted target object
   */
  <T> T convert(Object source, Class<?> sourceClass, Class<T> targetClass);

  /**
   * Convert the given object to target object in the given type.
   *
   * @param source object to be converted
   * @param object object helps to convert the source object
   * @param sourceClass source object type (mainly used to reference to the subscribed type)
   * @param targetClass target object class type
   * @param <T>    target object type
   * @return converted target object
   */
  <T> T convert(Object source, Object object, Class<?> sourceClass, Class<T> targetClass);

  /**
   * Convert the given object list into a list of the given target type.
   *
   * @param source a list of objects to be converted
   * @param targetClass target list element's class type
   * @param <T>    target list element's object type
   * @return a list of converted target objects
   */
  <T> List<T> convertCollection(List<?> source, Class<T> targetClass);

  /**
   * Convert the given object list into a list of the given target type.
   *
   * @param source a list of objects to be converted
   * @param object common object helps to convert the source objects
   * @param targetClass target list element's class type
   * @param <T>    target list element's object type
   * @return a list of converted target objects
   */
  <T> List<T> convertCollection(List<?> source, Object object, Class<T> targetClass);

  /**
   * Convert the given object list into a list of the given target type.
   *
   * @param source a list of objects to be converted
   * @param sourceClass source object type (mainly used to reference to the subscribed type)
   * @param targetClass target list element's class type
   * @param <T>    target list element's object type
   * @return a list of converted target objects
   */
  <T> List<T> convertCollection(List<?> source, Class<?> sourceClass, Class<T> targetClass);

  /**
   * Convert the given object list into a list of the given target type.
   *
   * @param source a list of objects to be converted
   * @param object common object helps to convert the source objects
   * @param sourceClass source object type (mainly used to reference to the subscribed type)
   * @param targetClass target list element's class type
   * @param <T>    target list element's object type
   * @return a list of converted target objects
   */
  <T> List<T> convertCollection(List<?> source, Object object, Class<?> sourceClass, Class<T> targetClass);
}
