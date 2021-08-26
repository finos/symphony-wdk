package com.symphony.bdk.workflow.swadl.validator;

import com.fasterxml.jackson.core.JsonPointer;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.CollectionNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.io.Reader;
import java.util.List;
import java.util.Optional;

/**
 * Inspired from https://github.com/zalando/zally/issues/78 to provide line numbers.
 */
public class YamlJsonPointer {

  private final Node yaml;

  public YamlJsonPointer(Reader input) {
    yaml = new Yaml().compose(input);
  }

  public Optional<Integer> getLine(JsonPointer pointer) {
    return Optional.ofNullable(find(yaml, null, pointer))
        .map(Node::getStartMark)
        .map(mark -> mark.getLine() + 1);
  }

  @SuppressWarnings("unchecked")
  private static Node find(Node node, Node parentNode, JsonPointer pointer) {
    if (pointer.tail() == null) {
      return parentNode;
    }
    if (node instanceof MappingNode) {
      return findMapping((MappingNode) node, pointer);
    } else if (node instanceof CollectionNode) {
      return findCollection((CollectionNode<Node>) node, pointer);
    }
    return null;
  }

  private static Node findMapping(MappingNode node, JsonPointer pointer) {
    for (NodeTuple value : node.getValue()) {
      Node key = value.getKeyNode();
      if (key instanceof ScalarNode && pointer.matchesProperty(((ScalarNode) key).getValue())) {
        return find(value.getValueNode(), key, pointer.tail());
      }
    }
    return null;
  }

  private static Node findCollection(CollectionNode<Node> tree, JsonPointer pointer) {
    List<Node> values = tree.getValue();
    for (int i = 0; i < values.size(); i++) {
      Node value = values.get(i);
      if (pointer.matchesElement(i)) {
        return find(value, value, pointer.tail());
      }
    }
    return null;
  }

}
