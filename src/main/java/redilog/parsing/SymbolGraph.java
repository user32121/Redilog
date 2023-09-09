package redilog.parsing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import net.minecraft.util.dynamic.Range;
import redilog.synthesis.LogicGraph;

/**
 * Represents the variables of a graph and how they relate to each other.<p>
 * This class is the intermediate between redilog code and a {@link LogicGraph},
 * and is reponsible for resolving values such as determining the range of all expressions.
 */
public class SymbolGraph {
    public Map<String, InputSExpression> inputs = new HashMap<>();
    public Map<String, OutputSExpression> outputs = new HashMap<>();
    public Map<String, SymbolExpression> expressions = new HashMap<>();

    public Map<String, Token> expressionDeclarations = new HashMap<>();

    /**
     * Ensure all {@link SymbolExpression expressions} have a nonempty value for their {@link SymbolExpression#range}
     * @throws RedilogParsingException
     */
    public void ResolveRanges() throws RedilogParsingException {
        //for secondary resolution due to dependencies
        Queue<Entry<String, SymbolExpression>> toProcess = new LinkedList<>();
        toProcess.addAll(expressions.entrySet());

        Entry<String, SymbolExpression> queueMarker = null;
        while (!toProcess.isEmpty()) {
            if (queueMarker == null) {
                queueMarker = toProcess.peek();
            } else if (queueMarker == toProcess.peek()) {
                throw new RedilogParsingException(
                        String.format("infinite loop detected for \"%s\" while resolving ranges",
                                queueMarker.getKey()));
            }
            Entry<String, SymbolExpression> entry = toProcess.remove();
            if (entry.getValue().range != null) {
                continue;
            }
            if (entry.getValue() instanceof OutputSExpression oe && oe.value != null) {
                if (oe.value.range == null) {
                    toProcess.add(entry);
                    continue;
                } else {
                    entry.getValue().range = oe.value.range;
                }
            } else {
                entry.getValue().range = new Range<Integer>(0, 0);
            }
            queueMarker = null;
        }
    }
}
