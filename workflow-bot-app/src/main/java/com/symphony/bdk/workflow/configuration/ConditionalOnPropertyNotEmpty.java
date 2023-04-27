package com.symphony.bdk.workflow.configuration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(ConditionalOnPropertyNotEmpty.OnPropertyNotEmptyCondition.class)
public @interface ConditionalOnPropertyNotEmpty {
  String value();

  class OnPropertyNotEmptyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      Map<String, Object> attrs = metadata.getAnnotationAttributes(ConditionalOnPropertyNotEmpty.class.getName());
      if (attrs == null) {
        return false;
      }
      String propertyName = (String) attrs.get("value");
      String property = context.getEnvironment().getProperty(propertyName);
      return property != null && StringUtils.isNoneBlank(property) && !"false".equals(property);
    }
  }
}
