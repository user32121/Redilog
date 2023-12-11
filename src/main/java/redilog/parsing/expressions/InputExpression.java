package redilog.parsing.expressions;

import net.minecraft.util.dynamic.Range;
import redilog.parsing.RedilogParsingException;
import redilog.parsing.Token;
import redilog.synthesis.nodes.ConstantNode;
import redilog.synthesis.nodes.InputNode;
import redilog.synthesis.nodes.Node;

public class InputExpression extends NamedExpression {

    public InputExpression(Token declaration, String name, Range<Integer> range) {
        super(declaration, name, range);
    }

    @Override
    public Node getNode(int index) {
        while (nodes.size() <= index) {
            nodes.add(null);
        }
        if (nodes.get(index) == null) {
            if (range != null && index > range.maxInclusive() - range.minInclusive()) {
                nodes.set(index, new ConstantNode(this, false));
            } else {
                nodes.set(index, new InputNode(this, String.format("%s[%s]", name, index + getRangeMin())));
            }
        }
        return nodes.get(index);
    }

    @Override
    public void setInput(Expression expression) throws RedilogParsingException {
        throw new RedilogParsingException(getClass() + " cannot be assigned");
    }

    @Override
    public Iterable<Node> getAllNodes() {
        return nodes;
    }

    @Override
    public int evaluateAsConstant() throws RedilogParsingException {
        throw new RedilogParsingException(
                String.format("%s does not have a constant value because it is an input", declaration));
    }
}