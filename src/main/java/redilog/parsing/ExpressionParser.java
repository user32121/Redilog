package redilog.parsing;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang3.NotImplementedException;

public class ExpressionParser {

    //higher means the operator applies first
    public static final Map<String, Integer> OPERATOR_PRECEDENCE = Map.of("|", 0);
    public static final Map<String, Boolean> OPERATOR_LEFT_ASSOCIATIVE = Map.of("|", true);

    public static Expression parseExpression(SymbolGraph graph, List<Token> tokens, int start, int end)
            throws RedilogParsingException {
        if (start > end) {
            throw new RedilogParsingException(String.format("Empty expression near %s", tokens.get(start)));
        }

        //https://en.wikipedia.org/wiki/Shunting_yard_algorithm
        Stack<Expression> output = new Stack<>();
        Stack<Token> operators = new Stack<>();
        for (int i = start; i <= end; ++i) {
            Token token = tokens.get(i);
            if (token.getType() == Token.Type.NUMBER) {
                //number
                ConstantExpression ce = new ConstantExpression(token, token.parseAsInt());
                output.add(ce);
                graph.expressions.put("Constant_" + token.getValue(), ce);
            } else if (token.getType() == Token.Type.VARIABLE) {
                //value
                String value = token.getValue();
                if (!graph.expressions.containsKey(value)) {
                    throw new RedilogParsingException(String.format("%s not defined", token));
                }
                output.add(graph.expressions.get(value));
            } else if (token.getType() == Token.Type.SYMBOL) {
                String o1 = token.getValue();
                if (OPERATOR_PRECEDENCE.containsKey(o1)) {
                    //operator
                    while (!operators.empty()) {
                        String o2 = operators.peek().getValue();
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
                    operators.push(token);
                } else if (o1.equals("(")) {
                    //left parenthesis
                    operators.push(token);
                } else if (o1.equals(")")) {
                    //right parenthesis
                    while (true) {
                        if (operators.empty()) {
                            throw new RedilogParsingException(
                                    String.format("Mismatched parentheses near %s", tokens.get(i)));
                        }
                        Token o2 = operators.pop();
                        if (o2.getValue().equals("(")) {
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
            Token t = operators.pop();
            if (t.getValue().equals("(")) {
                throw new RedilogParsingException(String.format("Mismatched parentheses, extra %s", t));
            }
            applyOperator(output, t, graph);
        }
        if (output.empty()) {
            throw new RedilogParsingException(
                    String.format("Expression starting at %s produced no output", tokens.get(start)));
        } else if (output.size() > 1) {
            throw new RedilogParsingException(String.format(
                    "Expression starting at %s produced multiple outputs, likely due to missing operators",
                    tokens.get(start)));
        }
        return output.pop();
    }

    private static void applyOperator(Stack<Expression> output, Token token, SymbolGraph graph)
            throws RedilogParsingException {
        String operator = token.getValue();
        if (operator.equals("|")) {
            if (output.size() < 2) {
                throw new RedilogParsingException(String.format("%s requires two operands", token));
            }
            Expression e1 = output.pop();
            Expression e2 = output.pop();
            output.push(new BitwiseOrExpression(token, e1, e2));
        } else {
            throw new NotImplementedException(operator + " not implemented");
        }
        graph.expressions.put(String.format("intermediate %s", output.peek()), output.peek());
    }
}
