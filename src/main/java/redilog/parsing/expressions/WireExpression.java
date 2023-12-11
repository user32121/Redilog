package redilog.parsing.expressions;

import com.google.common.collect.Iterables;

import net.minecraft.util.dynamic.Range;
import redilog.parsing.RedilogParsingException;
import redilog.parsing.Token;
import redilog.synthesis.nodes.IntermediateNode;
import redilog.synthesis.nodes.Node;

public class WireExpression extends NamedExpression {
    public Expression input;

    public WireExpression(Token declaration, String name, Range<Integer> range) {
        super(declaration, name, range);
    }

    @Override
    public Node getNode(int index) {
        if (range != null) {
            index = Math.min(index, range.maxInclusive() - range.minInclusive() + 1);
        }
        while (nodes.size() <= index) {
            nodes.add(null);
        }
        if (nodes.get(index) == null) {
            Node nodeValue = input != null ? input.getNode(index) : null;
            IntermediateNode node = new IntermediateNode(this, nodeValue);
            nodes.set(index, node);
        }
        return nodes.get(index);
    }

    @Override
    public void setInput(Expression expression) throws RedilogParsingException {
        if (input != null) {
            throw new RedilogParsingException(String.format("%s assigned twice", declaration));
        }
        input = expression;
    }

    @Override
    public void setUsed(int index) {
        super.setUsed(index);
        if (input != null) {
            input.setUsed(index);
        }
    }

    @Override
    public Iterable<Node> getAllNodes() {
        for (int i = 0; i < nodes.size(); ++i) {
            getNode(i);
        }
        if (input == null) {
            return nodes;
        }
        return Iterables.concat(nodes, input.getAllNodes());
    }

    @Override
    public int resolveRange() {
        //TODO stackoverflow for self referencing expressions
        if (range == null && input != null) {
            return Math.max(super.resolveRange(), input.resolveRange());
        } else {
            return super.resolveRange();
        }
    }
}
