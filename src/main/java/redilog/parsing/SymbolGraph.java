package redilog.parsing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import net.minecraft.util.dynamic.Range;
import redilog.synthesis.LogicGraph;
import redilog.synthesis.RedilogSynthesisException;

/**
 * Represents the variables of a graph and how they relate to each other.<p>
 * This class is the intermediate between redilog code and a {@link LogicGraph},
 * and is reponsible for resolving values such as determining the range of all expressions.
 */
public class SymbolGraph {
    public Map<String, InputExpression> inputs = new HashMap<>();
    public Map<String, OutputExpression> outputs = new HashMap<>();
    public Map<String, Expression> expressions = new HashMap<>();

    public Map<String, Token> expressionDeclarations = new HashMap<>();

    /**
     * Ensure all {@link Expression expressions} have a nonempty value for their {@link Expression#range}
     */
    public void ResolveRanges() throws RedilogSynthesisException {
        //for secondary resolution due to dependencies
        Queue<Entry<String, Expression>> toProcess = new LinkedList<>();
        toProcess.addAll(expressions.entrySet());

        Entry<String, Expression> queueMarker = null;
        while (!toProcess.isEmpty()) {
            if (queueMarker == null) {
                queueMarker = toProcess.peek();
            } else if (queueMarker == toProcess.peek()) {
                throw new RedilogSynthesisException(
                        String.format("infinite loop detected for \"%s\" while resolving ranges",
                                queueMarker.getKey()));
            }
            Entry<String, Expression> entry = toProcess.remove();
            if (entry.getValue().range != null) {
                continue;
            }
            if (entry.getValue() instanceof OutputExpression oe && oe.value != null) {
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
