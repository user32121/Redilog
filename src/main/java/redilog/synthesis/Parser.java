package redilog.synthesis;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    public static Graph parseRedilog(String redilog) throws RedilogParsingException {
        redilog = stripComments(redilog);
        List<Token> tokens = tokenize(redilog);
        Redilog.LOGGER.info("{}", tokens);
        return processTokens(tokens);
    }

    private static String stripComments(String input) {
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

    private static Graph processTokens(List<Token> tokens) throws RedilogParsingException {
        Graph graph = new Graph();
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

    private static int processDeclaration(Graph graph, List<Token> tokens, int i) throws RedilogParsingException {
        String variableType = tokens.get(i++).getValue(Token.Type.KEYWORD);

        //(optional) data range
        Optional<Range<Integer>> range = Optional.empty();
        if (tokens.get(i).getType() == Token.Type.SYMBOL) {
            tokens.get(i++).require(Token.Type.SYMBOL, "[");
            int rangeMax = tokens.get(i++).parseAsInt();
            tokens.get(i++).require(Token.Type.SYMBOL, ":");
            int rangeMin = tokens.get(i++).parseAsInt();
            tokens.get(i++).require(Token.Type.SYMBOL, "]");
            range = Optional.of(new Range<>(rangeMin, rangeMax));
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
            Node node = new Node(range);
            String name = variable.getLeft();
            Token declarer = variable.getRight();
            if (graph.nodes.containsKey(name)) {
                throw new RedilogParsingException(
                        String.format("%s already defined at %s", declarer, graph.nodeDeclarations.get(name)));
            }
            if (variableType.equals("input")) {
                graph.inputs.put(name, node);
            } else if (variableType.equals("output")) {
                graph.outputs.put(name, node);
            }
            graph.nodes.put(name, node);
            graph.nodeDeclarations.put(name, declarer);
        }

        return i;
    }

    private static int processAssignment(Graph graph, List<Token> tokens, int i) throws RedilogParsingException {
        tokens.get(i++).require(Token.Type.KEYWORD, "assign");

        String name = tokens.get(i++).getValue(Token.Type.VARIABLE);
        tokens.get(i++).require(Token.Type.SYMBOL, "=");
        //TODO expression parser
        String value = tokens.get(i++).getValue(Token.Type.VARIABLE);
        tokens.get(i++).require(Token.Type.SYMBOL, ";");

        if (!graph.nodes.containsKey(name)) {
            throw new RedilogParsingException(String.format("\"%s\" not defined", name));
        }
        if (!graph.nodes.containsKey(value)) {
            throw new RedilogParsingException(String.format("\"%s\" not defined", value));
        }
        if (graph.inputs.containsKey(name)) {
            throw new RedilogParsingException(String.format("input \"%s\" cannot be assigned", name));
        }
        graph.nodes.get(name).value = graph.nodes.get(value);
        return i;
    }
}
