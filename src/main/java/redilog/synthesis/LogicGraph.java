package redilog.synthesis;

import java.util.HashMap;
import java.util.Map;

import redilog.parsing.Token;

/**
 * Represents all the wires and logic gates in a graph.
 * This class can be fed to the {@link redilog.routing.Placer Placer} for placement and routing.
 */
public class LogicGraph {
    public Map<String, InputLExpression> inputs = new HashMap<>();
    public Map<String, OutputLExpression> outputs = new HashMap<>();
    public Map<String, LogicExpression> expressions = new HashMap<>();

    public Map<String, Token> expressionDeclarations = new HashMap<>();
}
