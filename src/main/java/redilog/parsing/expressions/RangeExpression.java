package redilog.parsing.expressions;

import net.minecraft.util.dynamic.Range;
import redilog.parsing.RedilogParsingException;
import redilog.parsing.Token;
import redilog.synthesis.RedilogSynthesisException;
import redilog.synthesis.nodes.Node;

/**
 * Although an expression, should only be used as an operand to {@link SliceExpression}, 
 * as this expression has no nodes and does not make sense as an operand to a standard operator (e.g. {@link BitwiseOrExpression}).
 */
public class RangeExpression extends Expression {
    public final Range<Integer> range;

    public RangeExpression(Token declaration, Expression low, Expression high) throws RedilogParsingException {
        super(declaration);
        this.range = new Range<>(low.evaluateAsConstant(), high.evaluateAsConstant());
    }

    @Override
    public int resolveRange() throws RedilogSynthesisException {
        throw new RedilogSynthesisException(String.format("Cannot use range %s as node", declaration));
    }

    @Override
    public Node getNode(int index) {
        throw new UnsupportedOperationException("Unsupported method 'getNode'");
    }

    @Override
    public void setInput(Expression expression) throws RedilogParsingException {
        throw new UnsupportedOperationException("Unsupported method 'setInput'");
    }

    @Override
    public Iterable<Node> getAllNodes() {
        throw new UnsupportedOperationException("Unsupported method 'getAllNodes'");
    }

    @Override
    public int evaluateAsConstant() throws RedilogParsingException {
        throw new UnsupportedOperationException("Unsupported method 'evaluateAsConstant'");
    }

}
