package redilog.synthesis;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.util.dynamic.Range;

/**
 * Represents all the wires and logic gates in a graph.
 * This class can be fed to the {@link redilog.routing.Placer Placer} for placement and routing.
 */
public class LogicGraph {
    public static interface Expression {
    }

    public static class Node implements Expression {
        public Expression value;

        public Node() {
        }

        public Node(Expression value) {
            this.value = value;
        }
    }

    public Map<String, Node> inputs = new HashMap<>();
    public Map<String, Node> outputs = new HashMap<>();
    public Map<String, Node> nodes = new HashMap<>();

    public Map<String, Token> nodeDeclarations = new HashMap<>();
}
