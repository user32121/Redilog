package redilog.synthesis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.util.Pair;
import net.minecraft.util.dynamic.Range;
import redilog.init.Redilog;
import redilog.synthesis.Token.Builder;
import redilog.synthesis.Token.TypeHint;

public class Parser {

    /**
     * Converts redilog code into a graph that relates the outputs to the inputs, similar to verilog synthesis.
     * @return a graph representation of the redilog
     * @throws RedilogParsingException
     */
    public static LogicGraph synthesizeRedilog(String redilog) throws RedilogParsingException {
        redilog = stripComments(redilog);
        List<Token> tokens = tokenize(redilog);
        SymbolGraph graph = processTokens(tokens);
        return convertGraph(graph);
    }

    private static String stripComments(String input) {
        //slash star
        while (true) {
            int start = input.indexOf("/*");
            if (start == -1) {
                break;
            }
            int end = input.indexOf("*/", start);
            if (end == -1) {
                end = input.length();
            } else {
                end += "*/".length();
            }
            input = input.substring(0, start)
                    + input.substring(start, end).replaceAll(".", "") //preserve line info (replace all nonline chars)
                    + input.substring(end);
        }
        //double slash
        while (true) {
            int start = input.indexOf("//");
            if (start == -1) {
                break;
            }
            int end = input.indexOf("\n", start);
            if (end == -1) {
                end = input.length();
            }
            input = input.substring(0, start) + input.substring(end);
        }
        return input;
    }

    private static List<Token> tokenize(String input) {
        List<Token> res = new ArrayList<>();

        Token.Builder token = null;
        int line = 1;
        int column = 1;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                //flush token
                if (token != null) {
                    res.add(token.build());
                    token = null;
                }
                if (c == '\n') {
                    ++line;
                    column = 1;
                }
                continue;
            } else if (Character.isLetterOrDigit(c)) {
                if (token == null) {
                    token = new Builder(TypeHint.LETTERS_DIGITS, line, column);
                } else if (token.getType() != TypeHint.LETTERS_DIGITS) {
                    res.add(token.build());
                    token = new Builder(TypeHint.LETTERS_DIGITS, line, column);
                }
                token.addChar(c);
            } else {
                if (token == null) {
                    token = new Builder(TypeHint.SYMBOL, line, column);
                } else if (token.getType() != TypeHint.SYMBOL) {
                    res.add(token.build());
                    token = new Builder(TypeHint.SYMBOL, line, column);
                }
                token.addChar(c);
            }
            ++column;
        }
        if (token != null) {
            res.add(token.build());
        }
        //make it so I don't need to check for range issues
        res.add(Token.EOF(line, column + 1));

        return res;
    }

    private static SymbolGraph processTokens(List<Token> tokens) throws RedilogParsingException {
        SymbolGraph graph = new SymbolGraph();
        int i = 0;
        while (i < tokens.size() && tokens.get(i).getType() != Token.Type.EOF) {
            String keyword = tokens.get(i).getValue(Token.Type.KEYWORD);
            if (keyword.equals("input") || keyword.equals("output")) {
                i = processDeclaration(graph, tokens, i);
            } else if (keyword.equals("assign")) {
                i = processAssignment(graph, tokens, i);
            } else {
                throw new NotImplementedException(String.format("%s handling not implemented", tokens.get(i)));
            }
        }
        return graph;
    }

    private static int processDeclaration(SymbolGraph graph, List<Token> tokens, int i) throws RedilogParsingException {
        String variableType = tokens.get(i++).getValue(Token.Type.KEYWORD);

        //(optional) data range
        Range<Integer> range = null;
        if (tokens.get(i).getType() == Token.Type.SYMBOL) {
            tokens.get(i++).require(Token.Type.SYMBOL, "[");
            int rangeMax = tokens.get(i++).parseAsInt();
            tokens.get(i++).require(Token.Type.SYMBOL, ":");
            int rangeMin = tokens.get(i++).parseAsInt();
            tokens.get(i++).require(Token.Type.SYMBOL, "]");
            range = new Range<>(rangeMin, rangeMax);
        }
        //variable names <name, token that declared it>
        List<Pair<String, Token>> newVariables = new ArrayList<>();
        while (true) {
            Token declarer = tokens.get(i++);
            String name = declarer.getValue(Token.Type.VARIABLE);
            newVariables.add(new Pair<String, Token>(name, declarer));
            tokens.get(i).require(Token.Type.SYMBOL, ",", ";");
            if (tokens.get(i++).getValue(Token.Type.SYMBOL).equals(";")) {
                break;
            }
        }
        for (Pair<String, Token> variable : newVariables) {
            SymbolGraph.Expression expression;
            String name = variable.getLeft();
            Token declarer = variable.getRight();
            if (graph.expressions.containsKey(name)) {
                throw new RedilogParsingException(
                        String.format("%s already defined at %s", declarer, graph.expressionDeclarations.get(name)));
            }
            if (variableType.equals("input")) {
                SymbolGraph.InputExpression ie = new SymbolGraph.InputExpression(range);
                graph.inputs.put(name, ie);
                expression = ie;
            } else if (variableType.equals("output")) {
                SymbolGraph.OutputExpression oe = new SymbolGraph.OutputExpression(range);
                graph.outputs.put(name, oe);
                expression = oe;
            } else {
                throw new NotImplementedException(variableType + " not implemented");
            }
            graph.expressions.put(name, expression);
            graph.expressionDeclarations.put(name, declarer);
        }

        return i;
    }

    private static int processAssignment(SymbolGraph graph, List<Token> tokens, int i) throws RedilogParsingException {
        tokens.get(i++).require(Token.Type.KEYWORD, "assign");

        String name = tokens.get(i++).getValue(Token.Type.VARIABLE);
        tokens.get(i++).require(Token.Type.SYMBOL, "=");
        //TODO expression parser
        String value = tokens.get(i++).getValue(Token.Type.VARIABLE);
        tokens.get(i++).require(Token.Type.SYMBOL, ";");

        if (!graph.expressions.containsKey(name)) {
            throw new RedilogParsingException(String.format("\"%s\" not defined", name));
        }
        if (!graph.expressions.containsKey(value)) {
            throw new RedilogParsingException(String.format("\"%s\" not defined", value));
        }
        if (graph.inputs.containsKey(name)) {
            throw new RedilogParsingException(String.format("input \"%s\" cannot be assigned", name));
        }
        if (graph.outputs.containsKey(value)) {
            throw new RedilogParsingException(String.format("output \"%s\" cannot be used as source", value));
        }
        if (graph.expressions.get(name) instanceof SymbolGraph.OutputExpression oe) {
            oe.value = graph.expressions.get(value);
        } else {
            throw new RedilogParsingException(String.format("expression \"%s\" (%s) cannot be assigned", name,
                    graph.expressions.get(name).getClass()));
        }
        return i;
    }

    private static LogicGraph convertGraph(SymbolGraph sGraph) throws RedilogParsingException {
        sGraph.ResolveRanges();

        LogicGraph lGraph = new LogicGraph();
        lGraph.expressionDeclarations = sGraph.expressionDeclarations;

        Map<SymbolGraph.Expression, String> names = new HashMap<>();
        for (Entry<String, SymbolGraph.Expression> symbol : sGraph.expressions.entrySet()) {
            names.put(symbol.getValue(), symbol.getKey());

            Range<Integer> range = symbol.getValue().range;
            //create a wire for each index
            for (int i = range.minInclusive(); i <= range.maxInclusive(); i++) {
                LogicGraph.Expression wire;
                String name = symbol.getKey() + "[" + i + "]";
                if (sGraph.inputs.containsKey(symbol.getKey())) {
                    LogicGraph.InputExpression ie = new LogicGraph.InputExpression();
                    lGraph.inputs.put(name, ie);
                    wire = ie;
                } else if (sGraph.outputs.containsKey(symbol.getKey())) {
                    LogicGraph.OutputExpression oe = new LogicGraph.OutputExpression();
                    lGraph.outputs.put(name, oe);
                    wire = oe;
                } else {
                    throw new NotImplementedException(symbol.getValue() + "not implemented");
                }
                lGraph.expressions.put(name, wire);
            }
        }

        //connect subexpressions (wires) (this has to occur in a separate loop to ensure all wires are already generated)
        for (Entry<String, SymbolGraph.Expression> symbol : sGraph.expressions.entrySet()) {
            if (symbol.getValue() instanceof SymbolGraph.OutputExpression soe) {
                Range<Integer> range = symbol.getValue().range;
                if (soe.value == null) {
                    Redilog.LOGGER.warn("symbol {} does not have a value", symbol.getKey());
                    continue;
                }
                Range<Integer> sourceRange = soe.value.range;
                for (int i = range.minInclusive(); i <= range.maxInclusive(); i++) {
                    String name = symbol.getKey() + "[" + i + "]";
                    String sourceName = names.get(soe.value) + "["
                            + (i - range.minInclusive() + sourceRange.minInclusive()) + "]";
                    LogicGraph.Expression wire = lGraph.expressions.get(name);
                    if (wire instanceof LogicGraph.OutputExpression loe) {
                        loe.value = lGraph.expressions.get(sourceName);
                    } else {
                        throw new NotImplementedException(wire + "not implemented");
                    }
                }
            }
        }

        return lGraph;
    }
}
