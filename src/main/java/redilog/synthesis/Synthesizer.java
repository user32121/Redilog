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
                        String.format("%s's unused check is not implemented", entry.getValue().getClass()));
            }
        }
    }

    private static void logWarnAndCreateMessage(Consumer<Text> feedback, String message) {
        Redilog.LOGGER.warn(message);
        feedback.accept(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
    }
}
