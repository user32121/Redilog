package redilog.synthesis;

import java.util.function.Consumer;

import net.minecraft.text.Text;
import redilog.blocks.BlockProgressBarManager;
import redilog.parsing.SymbolGraph;
import redilog.parsing.expressions.NamedExpression;
import redilog.synthesis.nodes.Node;
import redilog.synthesis.nodes.OutputNode;
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
        for (NamedExpression ne : sGraph.expressions) {
            for (Node n : ne.getAllNodes()) {
                lGraph.nodes.add(n);
            }
        }

        return lGraph;
    }

    private static void warnUnused(LogicGraph graph, Consumer<Text> feedback) {
        for (Node node : graph.nodes) {
            if (!node.used) {
                LoggerUtil.logWarnAndCreateMessage(feedback,
                        String.format("Value of node %s is not used", node.owner.nodeAsString(node)));
            }
            if (node instanceof OutputNode on) {
                if (on.value == null) {
                    LoggerUtil.logWarnAndCreateMessage(feedback,
                            String.format("Output %s has no value", node.owner.nodeAsString(node)));
                }
            }
        }
    }
}
