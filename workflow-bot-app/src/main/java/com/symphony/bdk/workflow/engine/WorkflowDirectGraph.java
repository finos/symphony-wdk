package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A direct graph class representing the SWADL workflow
 *
 * @see Workflow
 */
public class WorkflowDirectGraph {
  /**
   * Dictionary map, workflow element id as key, element itself as value
   */
  @Getter(AccessLevel.PACKAGE)
  private final Map<String, WorkflowNode> dictionary = new HashMap<>();
  /**
   * Graph map, workflow element id as key, children elements list as value
   */
  private final Map<String, NodeChildren> graph = new HashMap<>();
  /**
   * Parents map, workflow element id as key, its parents element ids as value
   */
  @Getter(AccessLevel.PACKAGE)
  private final Map<String, Set<String>> parents = new HashMap<>();

  /**
   * Workflow start events list
   */
  @Getter
  private final List<String> startEvents = new ArrayList<>();

  public void addParent(String id, String parent) {
    parents.computeIfAbsent(id, k -> new HashSet<>()).add(parent);
  }

  public void addStartEvent(String startEvent) {
    startEvents.add(startEvent);
  }

  public void registerToDictionary(String id, WorkflowNode node) {
    dictionary.put(id, node);
  }

  public boolean isRegistered(String id) {
    return dictionary.containsKey(id);
  }

  public NodeChildren getChildren(String id) {
    return graph.computeIfAbsent(id, k -> new NodeChildren());
  }

  public WorkflowNode readWorkflowNode(String id) {
    return dictionary.get(id);
  }

  public NodeChildren readChildren(String id) {
    return graph.get(id);
  }

  public List<String> getParents(String id) {
    return parents.get(id) == null ? new ArrayList<>() : new ArrayList<>(parents.get(id));
  }

  public enum Gateway {
    EXCLUSIVE,
    EVENT_BASED,
    PARALLEL
  }


  @NoArgsConstructor
  @AllArgsConstructor
  public static class NodeChildren {
    @Getter
    private Gateway gateway;
    @Getter
    private List<String> children = new ArrayList<>();

    public NodeChildren(List<String> children) {
      this.children = children;
    }

    public NodeChildren addChild(String child) {
      this.children.add(child);
      return this;
    }

    public NodeChildren removeChild(String child) {
      this.children.remove(child);
      return this;
    }

    public NodeChildren gateway(Gateway gateway) {
      this.gateway = gateway;
      return this;
    }

    public boolean isEmpty() {
      return children.isEmpty();
    }

    public boolean isChildUnique() {
      return children.size() == 1;
    }

    public String getUniqueChild() {
      if (isChildUnique()) {
        return children.get(0);
      } else {
        throw new IllegalStateException("No children or not unique child");
      }
    }
  }
}
