package redilog.parsing;

import net.minecraft.util.dynamic.Range;
import redilog.synthesis.InputNode;
import redilog.synthesis.Node;

public class InputExpression extends NamedExpression {

    public InputExpression(Token declaration, String name, Range<Integer> range) {
        super(declaration, name, range);
    }

    @Override
    public Node getNode(int index) throws IndexOutOfBoundsException {
        if (range != null) {
            index = Math.min(index, range.maxInclusive() - range.minInclusive() + 1);
        }
        while (nodes.size() <= index) {
            nodes.add(null);
        }
        if (nodes.get(index) == null) {
            nodes.set(index, new InputNode(this, String.format("%s[%s]", name, index + getRangeMin())));
        }
        return nodes.get(index);
    }

    @Override
    public void setValue(Expression expression) throws RedilogParsingException {
        throw new RedilogParsingException(getClass() + " cannot be assigned");
    }

    @Override
    public Iterable<Node> getAllNodes() {
        return nodes;
    }
}