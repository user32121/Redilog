package redilog.parsing;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang3.NotImplementedException;

import redilog.init.Redilog;

public class ExpressionParser {

    //higher means the operator applies first
    public static final Map<String, Integer> OPERATOR_PRECEDENCE = Map.of("|", 0);
    public static final Map<String, Boolean> OPERATOR_LEFT_ASSOCIATIVE = Map.of("|", true);

    public static Expression parseExpression(SymbolGraph graph, List<Token> tokens, int start, int end)
            throws RedilogParsingException {
        // TODO line numbers for error messages
        if (start > end) {
            throw new RedilogParsingException("Empty expression");
        }

        //https://en.wikipedia.org/wiki/Shunting_yard_algorithm
        Stack<Expression> output = new Stack<>();
        Stack<String> operators = new Stack<>();
        for (int i = start; i <= end; ++i) {
            if (tokens.get(i).getType() == Token.Type.NUMBER) {
                //number
                output.add(new ConstantExpression(tokens.get(i).parseAsInt()));
            } else if (tokens.get(i).getType() == Token.Type.VARIABLE) {
                //"number"
                String value = tokens.get(i).getValue();
                if (!graph.expressions.containsKey(value)) {
                    throw new RedilogParsingException(String.format("\"%s\" not defined", tokens.get(i)));
                }
                output.add(graph.expressions.get(value));
            } else if (tokens.get(i).getType() == Token.Type.SYMBOL) {
                String o1 = tokens.get(i).getValue();
                if (OPERATOR_PRECEDENCE.containsKey(o1)) {
                    //operator
                    while (!operators.empty()) {
                        String o2 = operators.peek();
                        boolean o2GtO1 = false, o2EqO1 = false;
                        boolean notLParen = !o2.equals("(");
                        if (notLParen) {
                            o2GtO1 = OPERATOR_PRECEDENCE.get(o2) > OPERATOR_PRECEDENCE.get(o1);
                            o2EqO1 = OPERATOR_PRECEDENCE.get(o2) == OPERATOR_PRECEDENCE.get(o1);
                        }
                        boolean leftAssoc = OPERATOR_LEFT_ASSOCIATIVE.get(o1);
                        if (!(notLParen && (o2GtO1 || (o2EqO1 && leftAssoc)))) {
                            break;
                        }
                        applyOperator(output, operators.pop(), graph);
                    }
                    operators.push(o1);
                } else if (o1.equals("(")) {
                    //left parenthesis
                    operators.push(o1);
                } else if (o1.equals(")")) {
                    //right parenthesis
                    while (true) {
                        if (operators.empty()) {
                            throw new RedilogParsingException(
                                    String.format("Mismatched parentheses near %s", tokens.get(i)));
                        }
                        String o2 = operators.pop();
                        if (o2.equals("(")) {
                            break;
                        }
                        applyOperator(output, o2, graph);
                    }
                } else {
                    throw new RedilogParsingException(String.format("Unrecognized symbol %s", tokens.get(i)));
                }
            }
        }
        while (!operators.empty()) {
            String o = operators.pop();
            if (o.equals("(")) {
                throw new RedilogParsingException(String.format("Mismatched parentheses, extra %s", o));
            }
            applyOperator(output, o, graph);
        }
        if (output.empty()) {
            throw new RedilogParsingException(String.format("Expression produced no output"));
        } else if (output.size() > 1) {
            throw new RedilogParsingException(
                    String.format("Expression produced multiple outputs, likely due to missing operators"));
        }
        return output.pop();
    }

    private static void applyOperator(Stack<Expression> output, String operator, SymbolGraph graph)
            throws RedilogParsingException {
        Redilog.LOGGER.info("{}", output);
        if (operator.equals("|")) {
            if (output.size() < 2) {
                throw new RedilogParsingException("| requires two operands");
            }
            Expression e1 = output.pop();
            Expression e2 = output.pop();
            output.push(new BitwiseOrExpression(e1, e2));
        } else {
            throw new NotImplementedException(operator + " not implemented");
        }
        graph.expressions.put(String.format("intermediate %s", output.peek()), output.peek());
    }
}
