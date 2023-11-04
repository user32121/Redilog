package redilog.synthesis;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents all the wires and logic gates in a graph.
 * This class can be fed to the {@link redilog.routing.Placer Placer} for placement and routing.
 */
public class LogicGraph {
    public Map<String, Node> nodes = new HashMap<>();
}
