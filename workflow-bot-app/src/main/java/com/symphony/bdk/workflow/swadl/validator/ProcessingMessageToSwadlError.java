package com.symphony.bdk.workflow.swadl.validator;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/*
 * A bit ugly but we want to offer better error messages for simple error cases and hide a bit the
 * JSON schema validation errors that are complex to read.
 */
@Slf4j
public class ProcessingMessageToSwadlError {

  private ProcessingMessageToSwadlError() {
    // utility class
  }

  static SwadlError convert(JsonNode yamlTree, YamlJsonPointer yamlJsonPointer,
      ProcessingMessage validationError) {

    try {
      JsonNode errorMap = validationError.asJson();
      // the keys are the JSON pointers to the error's location, the more '/ in it, the more precise it is
      TreeMap<String, JsonNode> errorsByLocation =
          new TreeMap<>(Comparator.comparingInt((String s) -> StringUtils.countMatches(s, '/'))
              .thenComparing(Function.identity()));
      searchMostSpecificError(errorMap, errorsByLocation);
      String mostSpecificErrorLocation = errorsByLocation.lastKey();

      JsonPointer jsonPointer = JsonPointer.compile(mostSpecificErrorLocation);
      Integer lineNumber = yamlJsonPointer.getLine(jsonPointer).orElse(-1);

      String message = createErrorMessage(yamlTree, validationError, errorsByLocation.lastEntry());
      return new SwadlError(lineNumber, message);

    } catch (Exception e) {
      log.debug("Got exception while building error, fallback to raw error message", e);
      // we do a bit a magic to build better error messages, so we prefer failures to do so to fail silently and fall
      // back to the message error provided by the JSON schema library
      return new SwadlError(-1, validationError.getMessage());
    }
  }

  private static String createErrorMessage(JsonNode yamlTree, ProcessingMessage validationError,
      Map.Entry<String, JsonNode> mostSpecificErrorEntry) {
    // defaults to the original error message in case we don't handle it
    String message = validationError.getMessage();

    // map the most specific error, either the first of an object or first of an array
    JsonNode mostSpecificError = mostSpecificErrorEntry.getValue().get(0);
    if (mostSpecificError == null) {
      mostSpecificError = mostSpecificErrorEntry.getValue();
    }

    // try to build a better error message from this error
    if (mostSpecificError != null && mostSpecificError.has("keyword")) {
      String erroredProperty = StringUtils.substringAfterLast(mostSpecificError.at("/instance/pointer").asText(), '/');
      if (StringUtils.isEmpty(erroredProperty)) {
        erroredProperty = "root";
      }

      String errorType = mostSpecificError.path("keyword").textValue();

      message = toErrorMessage(mostSpecificError, erroredProperty, errorType, yamlTree, message);

      if (errorType.equals("allOf") || errorType.equals("oneOf")) {
        // this a compound error, try to drill down to find the first one
        message = drillDownReports(message, mostSpecificError, erroredProperty, yamlTree);
      }
    }
    return message;
  }

  private static String toErrorMessage(JsonNode errorNode, String erroredProperty, String errorType,
      JsonNode yamlTree, String message) {
    if (errorType.equals("additionalProperties")) {
      return String.format("Unknown property %s",
          StringUtils.wrapIfMissing(errorNode.at("/unwanted/0").asText(), "'"));
    }

    if (errorType.equals("required")) {
      return String.format("Missing property %s for %s object",
          StringUtils.wrapIfMissing(errorNode.at("/missing/0").asText(), "'"),
          erroredProperty);
    }

    if (errorType.equals("pattern")) {
      return String.format("Invalid property %s, must match pattern %s",
          StringUtils.wrapIfMissing(erroredProperty, "'"),
          errorNode.path("regex").asText());
    }

    if (errorType.equals("type")) {
      JsonNode at = yamlTree.at(errorNode.at("/instance/pointer").asText());
      if (at.isTextual()) {
        // in case property is a string instead of an object
        erroredProperty = at.asText();
      }

      return String.format("Invalid property %s, expecting %s type, got %s",
          StringUtils.wrapIfMissing(erroredProperty, "'"),
          errorNode.at("/expected/0").asText(), errorNode.at("/found").asText());
    }

    if (errorType.equals("oneOf")) {
      JsonNode at = yamlTree.at(errorNode.at("/instance/pointer").asText());
      String property = "";
      if (at.isObject()) {
        property = at.fieldNames().next();
      }
      return String.format("Unknown property %s for %s object",
          StringUtils.wrapIfMissing(property, "'"), erroredProperty);
    }

    return message;
  }

  private static String drillDownReports(String message, JsonNode value, String propertyName, JsonNode yamlTree) {
    for (JsonNode report : value.get("reports")) {
      for (JsonNode nestedReport : report) {
        String errorType = nestedReport.path("keyword").asText();
        return toErrorMessage(nestedReport, propertyName, errorType, yamlTree, message);
      }
    }
    return message;
  }

  private static void searchMostSpecificError(JsonNode error, Map<String, JsonNode> locations) {
    if (locations.isEmpty()) {
      // capture errors at root level
      String location = error.at("/instance/pointer").asText();
      locations.put(location, error);
      if (StringUtils.isEmpty(location)) {
        // pointer is not set for unknown properties, try to rebuilt one
        JsonNode unwantedProperty = error.at("/unwanted/0");
        if (!unwantedProperty.isMissingNode()) {
          locations.put("/" + unwantedProperty.asText(), error);
        }
      }
    }

    // drill down the nested reported errors
    if (error.has("reports")) {
      for (JsonNode jsonNode : error.get("reports")) {
        if (jsonNode.isArray()) {
          for (JsonNode node : jsonNode) {
            JsonNode location = node.path("instance").path("pointer");
            if (!location.isMissingNode() && !locations.containsKey(location.textValue())) {
              locations.put(location.textValue(), jsonNode);
            }
            searchMostSpecificError(node, locations);
          }
        }
      }
    }
  }
}
