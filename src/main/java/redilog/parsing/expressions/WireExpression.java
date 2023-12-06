package redilog.parsing.expressions;

import com.google.common.collect.Iterables;

import net.minecraft.util.dynamic.Range;
import redilog.parsing.RedilogParsingException;
import redilog.parsing.Token;
import redilog.synthesis.nodes.Node;
import redilog.synthesis.nodes.OutputNode;

public class WireExpression extends NamedExpression {
    public Expression value;

    public WireExpression(Token declaration, String name, Range<Integer> range) {
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
            Node nodeValue = value != null ? value.getNode(index) : null;
            OutputNode node = new OutputNode(this, String.format("%s[%s]", name, index + getRangeMin()), nodeValue);
            nodes.set(index, node);
        }
        return nodes.get(index);
    }

    @Override
    public void setValue(Expression expression) throws RedilogParsingException {
        value = expression;
    }

    @Override
    public void setUsed(int index) {
        super.setUsed(index);
        if (value != null) {
            value.setUsed(index);
        }
    }

    @Override
    public Iterable<Node> getAllNodes() {
        for (int i = 0; i < nodes.size(); ++i) {
            getNode(i);
        }
        return Iterables.concat(nodes, value.getAllNodes());
    }

    @Override
    public int resolveRange() {
        if (range == null) {
            return Math.max(super.resolveRange(), value.resolveRange());
        } else {
            return super.resolveRange();
        }
    }
}
