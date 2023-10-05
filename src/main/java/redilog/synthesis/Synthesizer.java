package redilog.synthesis;

import java.util.Map.Entry;
import java.util.function.Consumer;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import redilog.init.Redilog;
import redilog.parsing.Expression;
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

        //ensure all needed nodes are loaded
        for (Entry<String, Expression> entry : sGraph.expressions.entrySet()) {
            Expression expression = entry.getValue();
            for (int i = 0; i <= expression.range.maxInclusive() - expression.range.minInclusive(); i++) {
                Node node = expression.getNode(i);
                String name = entry.getKey() + "[" + i + "]";
                lGraph.expressions.put(name, node);
                if (node instanceof InputNode in) {
                    lGraph.inputs.put(name, in);
                } else if (node instanceof OutputNode on) {
                    lGraph.outputs.put(name, on);
                }
            }
        }

        return lGraph;
    }

    private static void warnUnused(LogicGraph graph, Consumer<Text> feedback) {
        for (Entry<String, Node> entry : graph.expressions.entrySet()) {
            if (!entry.getValue().used) {
                logWarnAndCreateMessage(feedback, String.format("Value of node %s is not used", entry.getKey()));
            }
            if (entry.getValue() instanceof OutputNode on) {
                if (on.value == null) {
                    logWarnAndCreateMessage(feedback, String.format("Output %s has no value", entry.getKey()));
                }
            }
        }
    }

    private static void logWarnAndCreateMessage(Consumer<Text> feedback, String message) {
        Redilog.LOGGER.warn(message);
        feedback.accept(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
    }
}
