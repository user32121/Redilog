package redilog.parsing;

import java.util.Map;
import java.util.Stack;
import java.util.function.BiConsumer;

import redilog.parsing.expressions.BitwiseAndExpression;
import redilog.parsing.expressions.BitwiseNotExpression;
import redilog.parsing.expressions.BitwiseOrExpression;
import redilog.parsing.expressions.Expression;

public class Operator {
    public final static Map<String, Operator> OPERATORS = Map.of(
            "|", new Operator(0, true, 2,
                    (token, stack) -> stack.push(new BitwiseOrExpression(token, stack.pop(), stack.pop()))),
            "&", new Operator(1, true, 2,
                    (token, stack) -> stack.push(new BitwiseAndExpression(token, stack.pop(), stack.pop()))),
            "~", new Operator(2, false, 1,
                    (token, stack) -> stack.push(new BitwiseNotExpression(token, stack.pop()))));

    //higher precendence means the operator applies first
    public final int precedence;
    public final boolean leftAssociative;
    private final int operands;
    private final BiConsumer<Token, Stack<Expression>> operation;

    public Operator(int precedence, boolean leftAssociative, int operands,
            BiConsumer<Token, Stack<Expression>> operation) {
        this.precedence = precedence;
        this.leftAssociative = leftAssociative;
        this.operands = operands;
        this.operation = operation;
    }

    public void applyOperator(Token token, Stack<Expression> stack) throws RedilogParsingException {
        if (stack.size() < operands) {
            throw new RedilogParsingException(String.format("%s requires %d operands", token, operands));
        }
        operation.accept(token, stack);
    }
}
