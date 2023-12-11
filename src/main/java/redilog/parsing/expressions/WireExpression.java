package redilog.parsing.expressions;

import java.util.Collections;

import com.google.common.collect.Iterables;

import net.minecraft.util.dynamic.Range;
import redilog.parsing.RedilogParsingException;
import redilog.parsing.Token;
import redilog.synthesis.RedilogSynthesisException;
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

    private boolean recursionDetectorSetUsed = false;

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

    private boolean recursionDetectorGetAllNodes = false;

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
    public int resolveRange() throws RedilogSynthesisException {
        if (range == null && input != null) {
            return Math.max(super.resolveRange(), getInputRange());
        } else {
            return super.resolveRange();
        }
    }

    private boolean recursionDetectorGetInputRange = false;

    private int getInputRange() throws RedilogSynthesisException {
        if (recursionDetectorGetInputRange) {
            return 1;
        }
        recursionDetectorGetInputRange = true;
        int r = input.resolveRange();
        recursionDetectorGetInputRange = false;
        return r;
    }

    private boolean recursionDetectorEvaluateAsConstant = false;

    @Override
    public int evaluateAsConstant() throws RedilogParsingException {
        if (recursionDetectorEvaluateAsConstant) {
            throw new RedilogParsingException(
                    String.format("%s does not have a constant value because it is recursive", declaration));
        }
        recursionDetectorEvaluateAsConstant = true;
        int value = input.evaluateAsConstant();
        recursionDetectorEvaluateAsConstant = false;
        return value;
    }
}
