package redilog.synthesis;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents all the wires and logic gates in a graph.
 * This class can be fed to the {@link redilog.routing.Placer Placer} for placement and routing.
 */
public class LogicGraph {
    public Set<Node> nodes = new HashSet<>();
}
