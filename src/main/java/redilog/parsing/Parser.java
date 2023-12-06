package redilog.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.text.Text;
import net.minecraft.util.dynamic.Range;
import redilog.blocks.BlockProgressBarManager;
import redilog.init.Redilog;
import redilog.parsing.expressions.Expression;
import redilog.parsing.expressions.InputExpression;
import redilog.parsing.expressions.NamedExpression;
import redilog.parsing.expressions.OutputExpression;

public class Parser {

    /**
     * Converts redilog code into a graph that relates the outputs to the inputs, similar to verilog synthesis.
     * @param feedback the function will add messages that should be relayed to the user
     * @param bbpbm
     * @return a graph representation of the redilog
     * @throws RedilogParsingException
     */
    public static SymbolGraph parseRedilog(String redilog, Consumer<Text> feedback,
            BlockProgressBarManager bbpbm) throws RedilogParsingException {
        redilog = stripComments(redilog);
        List<Token> tokens = tokenize(redilog);
        SymbolGraph sGraph = processTokens(tokens);
        return sGraph;
    }

    private static String stripComments(String input) {
        //slash star (block comment)
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
                    + input.substring(start, end).replaceAll(".", " ") //preserve line info (replace all nonline chars)
                    + input.substring(end);
        }
        //double slash (line comment)
        while (true) {
            int start = input.indexOf("//");
            if (start == -1) {
                break;
            }
            int end = input.indexOf("\n", start);
            if (end == -1) {
                end = input.length();
            }
            input = input.substring(0, start)
                    + input.substring(start, end).replaceAll(".", " ")
                    + input.substring(end);
        }
        return input;
    }

    private static List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();

        int line = 1;
        int column = 1;
        int cur = 0;
        //for handling unary vs binary minus operator
        boolean lastTokenIsValue = false;
        while (cur < input.length()) {
            Matcher m;
            if (Character.isWhitespace(input.charAt(cur))) {
                //whitespace
                ++column;
                if (input.charAt(cur) == '\n') {
                    ++line;
                    column = 1;
                }
                ++cur;
                lastTokenIsValue = false;
            } else if ((m = Token.WORD.matcher(input)).find(cur) && m.start() == cur) {
                //word
                tokens.add(Token.word(m.group(), line, column));
                cur += m.end() - m.start();
                column += m.end() - m.start();
                lastTokenIsValue = true;
            } else if ((m = (lastTokenIsValue ? Token.POSITIVE_NUMBER : Token.NUMBER).matcher(input)).find(cur)
                    && m.start() == cur) {
                //number
                tokens.add(new Token(m.group(), Token.Type.NUMBER, line, column));
                cur += m.end() - m.start();
                column += m.end() - m.start();
                lastTokenIsValue = true;
            } else {
                //symbol
                String symbol = "_";
                for (String s : Token.MULTICHAR_SYMBOLS) {
                    if (input.startsWith(s, cur)) {
                        symbol = s;
                    }
                }
                tokens.add(new Token(input.substring(cur, cur + symbol.length()), Token.Type.SYMBOL, line, column));
                cur += symbol.length();
                column += symbol.length();
                lastTokenIsValue = false;
            }
        }

        //make it so I don't need to check for range issues
        tokens.add(Token.EOF(line, column + 1));

        Redilog.LOGGER.info("{}", tokens);
        return tokens;
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
        List<Token> newVariables = new ArrayList<>();
        while (true) {
            Token declarer = tokens.get(i++);
            newVariables.add(declarer);
            tokens.get(i).require(Token.Type.SYMBOL, ",", ";");
            if (tokens.get(i++).getValue(Token.Type.SYMBOL).equals(";")) {
                break;
            }
        }
        for (Token token : newVariables) {
            NamedExpression expression;
            String name = token.getValue(Token.Type.VARIABLE);
            for (NamedExpression ne : graph.expressions) {
                if (ne.name.equals(name)) {
                    throw new RedilogParsingException(String.format("%s already defined at %s", token, ne.declaration));
                }
            }
            if (variableType.equals("input")) {
                InputExpression ie = new InputExpression(token, name, range);
                expression = ie;
            } else if (variableType.equals("output")) {
                OutputExpression oe = new OutputExpression(token, name, range);
                expression = oe;
            } else {
                throw new NotImplementedException(variableType + " not implemented");
            }
            graph.expressions.add(expression);
        }

        return i;
    }

    private static int processAssignment(SymbolGraph graph, List<Token> tokens, int i) throws RedilogParsingException {
        //TODO prevent double assignment    
        tokens.get(i++).require(Token.Type.KEYWORD, "assign");

        Token name = tokens.get(i++);
        tokens.get(i++).require(Token.Type.SYMBOL, "=");
        //find next semicolon (or keyword that indicates missing semicolon)
        int j = i;
        while (true) {
            Token token = tokens.get(j);
            if (token.getType() == Token.Type.KEYWORD || token.getType() == Token.Type.EOF) {
                throw new RedilogParsingException(
                        String.format("\";\" expected between %s and %s", token, tokens.get(i - 1)));
            } else if (token.getType() == Token.Type.SYMBOL && token.getValue().equals(";")) {
                break;
            }
            ++j;
        }
        Expression expression = ExpressionParser.parseExpression(graph, tokens, i, j - 1);

        for (NamedExpression ne : graph.expressions) {
            if (ne.name.equals(name.getValue(Token.Type.VARIABLE))) {
                ne.setValue(expression);
                return j + 1;
            }
        }
        throw new RedilogParsingException(String.format("%s not defined", name));
    }
}
