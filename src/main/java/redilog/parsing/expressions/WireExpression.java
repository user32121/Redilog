package redilog.parsing.expressions;

import java.util.Collections;

import com.google.common.collect.Iterables;

import net.minecraft.util.dynamic.Range;
import redilog.parsing.RedilogParsingException;
import redilog.parsing.Token;
import redilog.synthesis.nodes.IntermediateNode;
import redilog.synthesis.nodes.Node;

public class WireExpression extends NamedExpression {
    public Expression input;
    private boolean recursionDetectorGetInputRange = false;
    private boolean recursionDetectorGetAllNodes = false;
    private boolean recursionDetectorSetUsed = false;

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
            IntermediateNode node = new IntermediateNode(this);
            nodes.set(index, node);
            Node nodeValue = input != null ? input.getNode(index) : null;
            node.setInput(nodeValue);
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
        if (recursionDetectorSetUsed) {
            return;
        }
        recursionDetectorSetUsed = true;
        super.setUsed(index);
        if (input != null) {
            input.setUsed(index);
        }
        recursionDetectorSetUsed = false;
    }

    @Override
    public Iterable<Node> getAllNodes() {
        if (recursionDetectorGetAllNodes) {
            return Collections.emptyList();
        }
        recursionDetectorGetAllNodes = true;
        for (int i = 0; i < nodes.size(); ++i) {
            getNode(i);
        }
        if (input == null) {
            return nodes;
        }
        Iterable<Node> it = Iterables.concat(nodes, input.getAllNodes());
        recursionDetectorGetAllNodes = false;
        return it;
    }

    @Override
    public int resolveRange() {
        if (range == null && input != null) {
            return Math.max(super.resolveRange(), getInputRange());
        } else {
            return super.resolveRange();
        }
    }

    private int getInputRange() {
        if (recursionDetectorGetInputRange) {
            return 1;
        }
        recursionDetectorGetInputRange = true;
        int r = input.resolveRange();
        recursionDetectorGetInputRange = false;
        return r;
    }
}
