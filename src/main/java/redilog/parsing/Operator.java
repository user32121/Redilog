package redilog.parsing;

import java.util.Map;
import java.util.Stack;

import redilog.parsing.expressions.BitwiseAndExpression;
import redilog.parsing.expressions.BitwiseNotExpression;
import redilog.parsing.expressions.BitwiseOrExpression;
import redilog.parsing.expressions.Expression;
import redilog.parsing.expressions.RangeExpression;
import redilog.parsing.expressions.SliceExpression;

public class Operator {
    //TODO more operators (+-!&&||==!=><)
    public final static Map<String, Operator> OPERATORS = Map.of(
            "|", new Operator(0, true, 2,
                    (token, stack) -> stack.push(new BitwiseOrExpression(token, stack.pop(), stack.pop()))),
            "&", new Operator(1, true, 2,
                    (token, stack) -> stack.push(new BitwiseAndExpression(token, stack.pop(), stack.pop()))),
            "~", new Operator(2, false, 1,
                    (token, stack) -> stack.push(new BitwiseNotExpression(token, stack.pop()))),
            //although index should be a unary postfix operator, we treat it like an binary infix operator
            "[", new Operator(-10, true, 2,
                    (token, stack) -> stack.push(new SliceExpression(token, stack.pop(), stack.pop()))),
            ":", new Operator(-5, true, 2,
                    (token, stack) -> stack.push(new RangeExpression(token, stack.pop(), stack.pop()))));

    //higher precendence means the operator applies first
    public final int precedence;
    public final boolean leftAssociative;
    private final int operands;
    private final OperatorOperation operation;

    public Operator(int precedence, boolean leftAssociative, int operands, OperatorOperation operation) {
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

    @FunctionalInterface
    public interface OperatorOperation {
        public void accept(Token token, Stack<Expression> stack) throws RedilogParsingException;
    }
}
