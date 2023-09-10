package redilog.synthesis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Range;
import redilog.init.Redilog;
import redilog.parsing.Expression;
import redilog.parsing.OutputExpression;
import redilog.parsing.SymbolGraph;

public class Synthesizer {
    public static LogicGraph synthesize(SymbolGraph sGraph, Consumer<Text> feedback) throws RedilogSynthesisException {
        LogicGraph lGraph = convertGraph(sGraph, feedback);
        warnUnused(lGraph, feedback);
        return lGraph;
    }

    private static LogicGraph convertGraph(SymbolGraph sGraph, Consumer<Text> feedback)
            throws RedilogSynthesisException {
        sGraph.ResolveRanges();

        LogicGraph lGraph = new LogicGraph();
        lGraph.expressionDeclarations = sGraph.expressionDeclarations;

        Map<Expression, String> names = new HashMap<>();
        for (Entry<String, Expression> symbol : sGraph.expressions.entrySet()) {
            names.put(symbol.getValue(), symbol.getKey());

            Range<Integer> range = symbol.getValue().range;
            //create a wire for each index
            for (int i = range.minInclusive(); i <= range.maxInclusive(); i++) {
                Node wire;
                String name = symbol.getKey() + "[" + i + "]";
                if (sGraph.inputs.containsKey(symbol.getKey())) {
                    InputNode ie = new InputNode();
                    lGraph.inputs.put(name, ie);
                    wire = ie;
                } else if (sGraph.outputs.containsKey(symbol.getKey())) {
                    OutputNode oe = new OutputNode();
                    lGraph.outputs.put(name, oe);
                    wire = oe;
                } else {
                    throw new NotImplementedException(symbol.getValue() + "not implemented");
                }
                lGraph.expressions.put(name, wire);
            }
        }

        //connect subexpressions (wires) (this has to occur in a separate loop to ensure all wires are already generated)
        for (Entry<String, Expression> symbol : sGraph.expressions.entrySet()) {
            if (symbol.getValue() instanceof OutputExpression soe) {
                Range<Integer> range = symbol.getValue().range;
                if (soe.value == null) {
                    logWarnAndCreateMessage(feedback,
                            String.format("Symbol %s does not have a value", symbol.getKey()));
                    continue;
                }
                Range<Integer> sourceRange = soe.value.range;
                for (int i = range.minInclusive(); i <= range.maxInclusive(); i++) {
                    String name = symbol.getKey() + "[" + i + "]";
                    String sourceName = names.get(soe.value) + "["
                            + (i - range.minInclusive() + sourceRange.minInclusive()) + "]";
                    Node wire = lGraph.expressions.get(name);
                    if (wire instanceof OutputNode loe) {
                        loe.value = lGraph.expressions.get(sourceName);
                        if (lGraph.expressions.get(sourceName) != null)
                            lGraph.expressions.get(sourceName).used = true;
                    } else {
                        throw new NotImplementedException(wire + "not implemented");
                    }
                }
            }
        }

        return lGraph;
    }

    private static void warnUnused(LogicGraph graph, Consumer<Text> feedback) {
        for (Entry<String, Node> entry : graph.expressions.entrySet()) {
            if (entry.getValue() instanceof InputNode ie) {
                if (!ie.used) {
                    logWarnAndCreateMessage(feedback, String.format("Value of input %s is not used", entry.getKey()));
                }
            } else if (entry.getValue() instanceof OutputNode oe) {
                if (oe.value == null) {
                    logWarnAndCreateMessage(feedback, String.format("Output %s has no value", entry.getKey()));
                }
            } else {
                logWarnAndCreateMessage(feedback,
                        String.format("unused check for %s not implemented", entry.getValue().getClass()));
            }
        }
    }

    private static void logWarnAndCreateMessage(Consumer<Text> feedback, String message) {
        Redilog.LOGGER.warn(message);
        feedback.accept(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
    }
}
