package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.message.exception.MessageParserException;
import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.MessageParser;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.workflow.engine.executor.EventHolder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.impl.javax.el.FunctionMapper;
import org.camunda.bpm.engine.impl.util.ReflectUtil;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for EL evaluation by Camunda.
 */
public class UtilityFunctionsMapper extends FunctionMapper {

  /**
   * How to call those functions from script tasks or from Freemarker templates.
   * Usage: wdk.text(...) from a script or ${wdk.text(...)} from a template.
   */
  public static final String WDK_PREFIX = "wdk";
  public static final String TEXT = "text";
  public static final String JSON = "json";
  public static final String ESCAPE = "escape";
  public static final String MENTIONS = "mentions";
  public static final String HASHTAGS = "hashTags";
  public static final String CASHTAGS = "cashTags";
  public static final String EMOJIS =  "emojis";

  private static final Map<String, Method> FUNCTION_MAP;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    FUNCTION_MAP = new HashMap<>();
    FUNCTION_MAP.put("text", ReflectUtil.getMethod(UtilityFunctionsMapper.class, "text", String.class));
    FUNCTION_MAP.put("json", ReflectUtil.getMethod(UtilityFunctionsMapper.class, "json", String.class));
    FUNCTION_MAP.put("escape", ReflectUtil.getMethod(UtilityFunctionsMapper.class, "escape", String.class));
    FUNCTION_MAP.put("mentions", ReflectUtil.getMethod(UtilityFunctionsMapper.class, "mentions", Object.class));
    FUNCTION_MAP.put("hashTags", ReflectUtil.getMethod(UtilityFunctionsMapper.class, "hashTags", Object.class));
    FUNCTION_MAP.put("cashTags", ReflectUtil.getMethod(UtilityFunctionsMapper.class, "cashTags", Object.class));
    FUNCTION_MAP.put("emojis", ReflectUtil.getMethod(UtilityFunctionsMapper.class, "emojis", Object.class));
  }

  @Override
  public Method resolveFunction(String prefix, String localName) {
    return FUNCTION_MAP.get(localName);
  }

  public static Object json(String string) {
    try {
      return objectMapper.readValue(string, Object.class);
    } catch (JsonProcessingException jsonProcessingException) {
      return string;
    }
  }

  public static String text(String presentationMl) throws PresentationMLParserException {
    return PresentationMLParser.getTextContent(presentationMl);
  }

  public static String escape(String s) {
    if (s == null) {
      return null;
    }
    return new String(JsonStringEncoder.getInstance().quoteAsString(s));
  }

  @SuppressWarnings("rawtypes")
  public static List<Long> mentions(Object event) throws MessageParserException {
    if (event instanceof EventHolder && ((EventHolder) event).getSource() instanceof V4MessageSent) {
      return MessageParser.getMentions(((V4MessageSent) ((EventHolder) event).getSource()).getMessage());
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("rawtypes")
  public static List<String> hashTags(Object event) throws MessageParserException {
    if (event instanceof EventHolder && ((EventHolder) event).getSource() instanceof V4MessageSent) {
      return MessageParser.getHashtags(((V4MessageSent) ((EventHolder) event).getSource()).getMessage());
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("rawtypes")
  public static List<String> cashTags(Object event) throws MessageParserException {
    if (event instanceof EventHolder && ((EventHolder) event).getSource() instanceof V4MessageSent) {
      return MessageParser.getCashtags(((V4MessageSent) ((EventHolder) event).getSource()).getMessage());
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("rawtypes")
  public static Map<String, String> emojis(Object event) throws MessageParserException {
    if (event instanceof EventHolder && ((EventHolder) event).getSource() instanceof V4MessageSent) {
      return MessageParser.getEmojis(((V4MessageSent) ((EventHolder) event).getSource()).getMessage());
    } else {
      return Collections.emptyMap();
    }
  }
}
