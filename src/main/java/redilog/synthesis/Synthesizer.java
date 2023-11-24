package redilog.synthesis;

import java.util.Map.Entry;
import java.util.function.Consumer;

import net.minecraft.text.Text;
import redilog.blocks.BlockProgressBarManager;
import redilog.init.Redilog;
import redilog.parsing.Expression;
import redilog.parsing.OutputExpression;
import redilog.parsing.SymbolGraph;
import redilog.utils.LoggerUtil;

public class Synthesizer {
    public static LogicGraph synthesize(SymbolGraph sGraph, Consumer<Text> feedback,
            BlockProgressBarManager bbpbm) throws RedilogSynthesisException {
        LogicGraph lGraph = convertGraph(sGraph, feedback);
        warnUnused(lGraph, feedback);
        return lGraph;
    }

    private static LogicGraph convertGraph(SymbolGraph sGraph, Consumer<Text> feedback)
            throws RedilogSynthesisException {
        sGraph.ResolveRanges();

        LogicGraph lGraph = new LogicGraph();

        //ensure all needed nodes are loaded
        for (Entry<String, Expression> entry : sGraph.expressions.entrySet()) {
            Expression expression = entry.getValue();
            for (int i = 0; i <= expression.range.maxInclusive() - expression.range.minInclusive(); i++) {
                Node node = expression.getNode(i);
                String name = entry.getKey() + "[" + i + "]";
                lGraph.nodes.put(name, node);
                if (expression instanceof OutputExpression) {
                    expression.setUsed(i);
                }
            }
        }

        return lGraph;
    }

    private static void warnUnused(LogicGraph graph, Consumer<Text> feedback) {
        for (Entry<String, Node> entry : graph.nodes.entrySet()) {
            Redilog.LOGGER.info("{}: {}", entry, entry.getValue().used);

            if (!entry.getValue().used) {
                LoggerUtil.logWarnAndCreateMessage(feedback,
                        String.format("Value of node %s is not used", entry.getKey()));
            }
            if (entry.getValue() instanceof OutputNode on) {
                if (on.value == null) {
                    LoggerUtil.logWarnAndCreateMessage(feedback,
                            String.format("Output %s has no value", entry.getKey()));
                }
            }
        }
    }
}
