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
 * and is reponsible for resolving values such as determining the range of all expressions.
 */
public class SymbolGraph {
    public static abstract class Expression {
        @Nullable
        public Range<Integer> range;

        public Expression(Range<Integer> range) {
            this.range = range;
        }
    }

    public static class InputExpression extends Expression {
        public InputExpression(Range<Integer> range) {
            super(range);
        }
    }

    public static class OutputExpression extends Expression {
        public Expression value;

        public OutputExpression(Range<Integer> range) {
            super(range);
        }
    }

    public Map<String, InputExpression> inputs = new HashMap<>();
    public Map<String, OutputExpression> outputs = new HashMap<>();
    public Map<String, Expression> expressions = new HashMap<>();

    public Map<String, Token> expressionDeclarations = new HashMap<>();

    /**
     * Ensure all {@link Expression expressions} have a nonempty value for their {@link Expression#range}
     * @throws RedilogParsingException
     */
    public void ResolveRanges() throws RedilogParsingException {
        //for secondary resolution due to dependencies
        Queue<Entry<String, Expression>> toProcess = new LinkedList<>();
        toProcess.addAll(expressions.entrySet());

        Entry<String, Expression> queueMarker = null;
        while (!toProcess.isEmpty()) {
            if (queueMarker == null) {
                queueMarker = toProcess.peek();
            } else if (queueMarker == toProcess.peek()) {
                throw new RedilogParsingException(
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
