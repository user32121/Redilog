package redilog.synthesis;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.util.dynamic.Range;

/**
 * Represents the variables of a graph and how they relate to each other.<p>
 * This class is the intermediate between redilog code and a {@link LogicGraph},
 * and is reponsible for resolving values such as determining the range of all nodes.
 */
public class SymbolGraph {
    public static interface Expression {
    }

    public static class Node implements Expression {
        public Optional<Range<Integer>> range;
        public Expression value;

        public Node(Optional<Range<Integer>> range) {
            this.range = range;
        }
    }

    public Map<String, Node> inputs = new HashMap<>();
    public Map<String, Node> outputs = new HashMap<>();
    public Map<String, Node> nodes = new HashMap<>();

    public Map<String, Token> nodeDeclarations = new HashMap<>();

    /**
     * Ensure all {@link Node nodes} have a nonempty value for their {@link Node#range}
     */
    public void ResolveRanges() {
        for (Node node : nodes.values()) {
            if (node.range.isEmpty()) {
                if (node.value instanceof Node n) {
                    node.range = n.range;
                } else {
                    node.range = Optional.of(new Range<Integer>(0, 0));
                }
            }
        }
    }
}
