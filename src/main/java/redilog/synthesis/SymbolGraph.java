package redilog.synthesis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.jetbrains.annotations.Nullable;

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
        @Nullable
        public Range<Integer> range;
        @Nullable
        public Expression value;

        public Node(Range<Integer> range) {
            this.range = range;
        }
    }

    public Map<String, Node> inputs = new HashMap<>();
    public Map<String, Node> outputs = new HashMap<>();
    public Map<String, Node> nodes = new HashMap<>();

    public Map<String, Token> nodeDeclarations = new HashMap<>();

    /**
     * Ensure all {@link Node nodes} have a nonempty value for their {@link Node#range}
     * @throws RedilogParsingException
     */
    public void ResolveRanges() throws RedilogParsingException {
        //for secondary resolution due to dependencies
        Queue<Entry<String, Node>> toProcess = new LinkedList<>();
        toProcess.addAll(nodes.entrySet());

        Entry<String, Node> queueMarker = null;
        while (!toProcess.isEmpty()) {
            if (queueMarker == null) {
                queueMarker = toProcess.peek();
            } else if (queueMarker == toProcess.peek()) {
                throw new RedilogParsingException(
                        String.format("infinite loop detected for \"%s\" while resolving ranges", queueMarker.getKey()));
            }
            Entry<String, Node> entry = toProcess.remove();
            if (entry.getValue().range != null) {
                continue;
            }
            if (entry.getValue().value instanceof Node source) {
                if (source.range == null) {
                    toProcess.add(entry);
                    continue;
                } else {
                    entry.getValue().range = source.range;
                }
            } else {
                entry.getValue().range = new Range<Integer>(0, 0);
            }
            queueMarker = null;
        }
    }
}
