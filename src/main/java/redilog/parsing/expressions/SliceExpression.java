package redilog.parsing.expressions;

import net.minecraft.util.dynamic.Range;
import redilog.parsing.RedilogParsingException;
import redilog.parsing.Token;
import redilog.synthesis.nodes.Node;

public class SliceExpression extends Expression {
    public Expression input;
    public final Range<Integer> range;

    public SliceExpression(Token declaration, Expression input, Expression range) throws RedilogParsingException {
        super(declaration);
        this.input = input;
        if (range instanceof RangeExpression re) {
            this.range = re.range;
        } else {
            int index = range.evaluateAsConstant();
            this.range = new Range<>(index, index);
        }
    }

    @Override
    public int resolveRange() {
        return range.maxInclusive() - range.minInclusive() + 1;
    }

    @Override
    public Node getNode(int index) {
        if (input instanceof NamedExpression ne) {
            return ne.getNodeWithAddress(index + range.minInclusive());
        } else {
            return input.getNode(index + range.minInclusive());
        }
    }

    @Override
    public void setInput(Expression expression) throws RedilogParsingException {
        this.input = expression;
    }

    @Override
    public Iterable<Node> getAllNodes() {
        return input.getAllNodes();
    }

    @Override
    public int evaluateAsConstant() throws RedilogParsingException {
        int value = input.evaluateAsConstant();
        if (input instanceof NamedExpression ne) {
            return (value & ((1 << range.maxInclusive() - ne.range.minInclusive() + 1) - 1)) >> (range.minInclusive()
                    - ne.range.minInclusive());
        } else {
            return (value & ((1 << range.maxInclusive() + 1) - 1)) >> range.minInclusive();
        }
    }
}
