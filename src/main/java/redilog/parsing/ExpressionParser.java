package redilog.parsing;

import java.util.List;
import java.util.Stack;

import redilog.parsing.expressions.ConstantExpression;
import redilog.parsing.expressions.Expression;
import redilog.parsing.expressions.NamedExpression;

public class ExpressionParser {

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
            } else if (token.getType() == Token.Type.VARIABLE) {
                //value
                String value = token.getValue();
                NamedExpression ne2 = null;
                for (NamedExpression ne : graph.expressions) {
                    if (ne.name.equals(value)) {
                        ne2 = ne;
                    }
                }
                if (ne2 == null) {
                    throw new RedilogParsingException(String.format("%s not defined", token));
                }
                output.add(ne2);
            } else if (token.getType() == Token.Type.SYMBOL) {
                String o1 = token.getValue();
                if (Operator.OPERATORS.containsKey(o1)) {
                    //operator
                    while (!operators.empty()) {
                        String o2 = operators.peek().getValue();
                        boolean o2GtO1 = false, o2EqO1 = false;
                        boolean notLParen = !o2.equals("(");
                        if (notLParen) {
                            o2GtO1 = Operator.OPERATORS.get(o2).precedence > Operator.OPERATORS.get(o1).precedence;
                            o2EqO1 = Operator.OPERATORS.get(o2).precedence == Operator.OPERATORS.get(o1).precedence;
                        }
                        boolean leftAssoc = Operator.OPERATORS.get(o1).leftAssociative;
                        if (!(notLParen && (o2GtO1 || (o2EqO1 && leftAssoc)))) {
                            break;
                        }
                        applyOperator(output, operators.pop());
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
                                    String.format("Mismatched parenthesis near %s", tokens.get(i)));
                        }
                        Token o2 = operators.pop();
                        if (o2.getValue().equals("(")) {
                            break;
                        }
                        applyOperator(output, o2);
                    }
                } else if (o1.equals("]")) {
                    //right bracket, terminate index operation
                    while (true) {
                        if (operators.empty()) {
                            throw new RedilogParsingException(
                                    String.format("Mismatched bracket near %s", tokens.get(i)));
                        }
                        Token o2 = operators.pop();
                        if (o2.getValue().equals("[")) {
                            break;
                        }
                        applyOperator(output, o2);
                    }
                } else {
                    throw new RedilogParsingException(String.format("Unrecognized symbol %s", tokens.get(i)));
                }
            }
        }
        while (!operators.empty()) {
            Token t = operators.pop();
            if (t.getValue().equals("(")) {
                throw new RedilogParsingException(String.format("Mismatched parenthesis, extra %s", t));
            }
            applyOperator(output, t);
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

    private static void applyOperator(Stack<Expression> output, Token token)
            throws RedilogParsingException {
        String operator = token.getValue();
        Operator.OPERATORS.get(operator).applyOperator(token, output);
    }
}
