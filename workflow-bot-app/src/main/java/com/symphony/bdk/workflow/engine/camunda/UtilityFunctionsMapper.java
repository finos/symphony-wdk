package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.message.exception.MessageParserException;
import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.MessageParser;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.workflow.engine.executor.EventHolder;
import com.symphony.bdk.workflow.engine.executor.SharedDataStore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.impl.javax.el.FunctionMapper;
import org.camunda.bpm.engine.impl.util.ReflectUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for EL evaluation by Camunda.
 */
public class UtilityFunctionsMapper extends FunctionMapper {
  private static SessionService staticSessionService;

  private static SharedDataStore sharedDataStore;

  public static void setStaticSessionService(SessionService sessionService) {
    UtilityFunctionsMapper.staticSessionService = sessionService;
  }

  public static void setSharedStateService(SharedDataStore sharedDataStore) {
    UtilityFunctionsMapper.sharedDataStore = sharedDataStore;
  }

  @Autowired
  public UtilityFunctionsMapper(SessionService sessionService, SharedDataStore sharedDataStore) {
    setStaticSessionService(sessionService);
    setSharedStateService(sharedDataStore);
  }

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
  public static final String EMOJIS = "emojis";
  public static final String SESSION = "session";
  public static final String READSHARED = "readShared";
  public static final String WRITESHARED = "writeShared";

  private static final Map<String, Method> FUNCTION_MAP;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    FUNCTION_MAP = new HashMap<>();
    FUNCTION_MAP.put(TEXT, ReflectUtil.getMethod(UtilityFunctionsMapper.class, TEXT, String.class));
    FUNCTION_MAP.put(JSON, ReflectUtil.getMethod(UtilityFunctionsMapper.class, JSON, String.class));
    FUNCTION_MAP.put(ESCAPE, ReflectUtil.getMethod(UtilityFunctionsMapper.class, ESCAPE, String.class));
    FUNCTION_MAP.put(MENTIONS, ReflectUtil.getMethod(UtilityFunctionsMapper.class, MENTIONS, Object.class));
    FUNCTION_MAP.put(HASHTAGS, ReflectUtil.getMethod(UtilityFunctionsMapper.class, HASHTAGS, Object.class));
    FUNCTION_MAP.put(CASHTAGS, ReflectUtil.getMethod(UtilityFunctionsMapper.class, CASHTAGS, Object.class));
    FUNCTION_MAP.put(EMOJIS, ReflectUtil.getMethod(UtilityFunctionsMapper.class, ESCAPE, Object.class));
    FUNCTION_MAP.put(SESSION, ReflectUtil.getMethod(UtilityFunctionsMapper.class, SESSION));
    FUNCTION_MAP.put(READSHARED,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, READSHARED, String.class, String.class));
    FUNCTION_MAP.put(WRITESHARED,
        ReflectUtil.getMethod(UtilityFunctionsMapper.class, WRITESHARED, String.class, String.class, Object.class));
  }

  @Override
  public Method resolveFunction(String prefix, String localName) {
    return FUNCTION_MAP.get(localName);
  }

  public static UserV2 session() {
    return staticSessionService.getSession();
  }

  public static Object json(String string) {
    try {
      return objectMapper.readValue(string, Object.class);
    } catch (JsonProcessingException jsonProcessingException) {
      return string;
    }
  }

  public static Object readShared(String namespace, String key) {
    return sharedDataStore.getNamespaceData(namespace).get(key);
  }

  public static void writeShared(String namespace, String key, Object data) {
    sharedDataStore.putNamespaceData(namespace, key, data);
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
