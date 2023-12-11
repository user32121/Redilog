package redilog.parsing.expressions;

import net.minecraft.util.dynamic.Range;
import redilog.parsing.RedilogParsingException;
import redilog.parsing.Token;
import redilog.synthesis.nodes.ConstantNode;
import redilog.synthesis.nodes.Node;

//TODO nulls occuring in input and outputs when using this
public class SliceExpression extends Expression {
    public Expression input;
    public final Range<Integer> range;

    public SliceExpression(Token declaration, Expression range, Expression input) throws RedilogParsingException {
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
        while (nodes.size() <= index) {
            nodes.add(null);
        }
        if (index + range.minInclusive() > range.maxInclusive()) {
            nodes.set(index, new ConstantNode(this, false));
        } else if (input instanceof NamedExpression ne) {
            nodes.set(index, ne.getNodeWithAddress(index + range.minInclusive()));
        } else {
            nodes.set(index, input.getNode(index + range.minInclusive()));
        }
        return nodes.get(index);
    }

    @Override
    public void setInput(Expression expression) throws RedilogParsingException {
        this.input = expression;
    }

    @Override
    public Iterable<Node> getAllNodes() {
        return nodes;
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
