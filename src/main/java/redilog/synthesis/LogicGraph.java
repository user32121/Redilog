package redilog.synthesis;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents all the wires and logic gates in a graph.
 * This class can be fed to the {@link redilog.routing.Placer Placer} for placement and routing.
 */
public class LogicGraph {
    public static interface Expression {
    }

    public static class InputExpression implements Expression {
    }

    public static class OutputExpression implements Expression {
        public Expression value;
    }

    public Map<String, InputExpression> inputs = new HashMap<>();
    public Map<String, OutputExpression> outputs = new HashMap<>();
    public Map<String, Expression> expressions = new HashMap<>();

    public Map<String, Token> expressionDeclarations = new HashMap<>();
}
