package com.symphony.bdk.workflow.lang;

import com.symphony.bdk.workflow.lang.swadl.Activity;
import com.symphony.bdk.workflow.lang.swadl.activity.BaseActivity;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * To support custom activities, found at runtime.
 */
public class ActivityDeserializer extends StdDeserializer<Activity> {

  public ActivityDeserializer() {
    super(Activity.class);
  }

  @Override
  public Activity deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ObjectMapper mapper = (ObjectMapper) p.getCodec();
    TreeNode node = mapper.readTree(p);

    try {
      String activityType = node.fieldNames().next();
      List<Class<? extends BaseActivity>> matchingActivityTypes = ActivityRegistry.getActivityTypes().stream()
          .filter(activityClass -> classNameMatches(activityType, activityClass))
          .collect(Collectors.toList());
      if (matchingActivityTypes.isEmpty()) {
        throw JsonMappingException.from(p,
            "Could not find an activity named: " + activityType
                + " from known activities (" + ActivityRegistry.getActivityTypes() + ")");
      }
      if (matchingActivityTypes.size() > 1) {
        throw JsonMappingException.from(p,
            "Found multiple activity types(" + matchingActivityTypes + ") for name: " + activityType);
      }

      BaseActivity baseActivity = mapper.treeToValue(node.get(activityType), matchingActivityTypes.get(0));

      Activity activity = new Activity();
      activity.setImplementation(baseActivity);
      return activity;
    } catch (NoSuchElementException e) {
      throw JsonMappingException.from(p, "No activity defined");
    }
  }

  private boolean classNameMatches(String property, Class<? extends BaseActivity> activityClass) {
    // property is kebab-case
    return activityClass.getSimpleName().toLowerCase().equals(StringUtils.remove(property, '-'));
  }

}
