package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.message.exception.MessageParserException;
import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.MessageParser;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.workflow.engine.executor.EventHolder;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
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
public class BdkFunctionMapper extends FunctionMapper {

  private static final Map<String, Method> FUNCTION_MAP;

  static {
    FUNCTION_MAP = new HashMap<>();
    FUNCTION_MAP.put("text", ReflectUtil.getMethod(BdkFunctionMapper.class, "text", String.class));
    FUNCTION_MAP.put("escape", ReflectUtil.getMethod(BdkFunctionMapper.class, "escape", String.class));
    FUNCTION_MAP.put("mentions", ReflectUtil.getMethod(BdkFunctionMapper.class, "mentions", Object.class));
    FUNCTION_MAP.put("hashTags", ReflectUtil.getMethod(BdkFunctionMapper.class, "hashTags", Object.class));
    FUNCTION_MAP.put("cashTags", ReflectUtil.getMethod(BdkFunctionMapper.class, "cashTags", Object.class));
    FUNCTION_MAP.put("emojis", ReflectUtil.getMethod(BdkFunctionMapper.class, "emojis", Object.class));
  }

  public Method resolveFunction(String prefix, String localName) {
    return FUNCTION_MAP.get(localName);
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
