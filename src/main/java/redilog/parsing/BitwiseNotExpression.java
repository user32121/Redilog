package redilog.parsing;

import net.minecraft.util.dynamic.Range;
import redilog.synthesis.Node;
import redilog.synthesis.NotNode;

public class BitwiseNotExpression extends Expression {
    public Expression input1;

    public BitwiseNotExpression(Token declaration, Expression input1) {
        super(declaration, null);
        this.input1 = input1;
    }

    private static int getMSBNeeded(Range<Integer> r1) {
        return r1.maxInclusive() - r1.minInclusive();
    }

    @Override
    public boolean resolveRange() {
        if (range != null) {
            return true;
        }
        if (input1.range == null) {
            return false;
        }
        range = new Range<>(0, getMSBNeeded(input1.range));
        return true;
    }

    @Override
    public Node getNode(int index) throws IndexOutOfBoundsException {
        if (nodes == null) {
            nodes = new Node[range.maxInclusive() - range.minInclusive() + 1];
        }
        if (nodes[index] == null) {
            NotNode node = new NotNode(this, input1.getNode(index));
            nodes[index] = node;
        }
        return nodes[index];
    }

    @Override
    public void setValue(Expression expression) throws RedilogParsingException {
        input1 = expression;
    }

    @Override
    public void setUsed(int index) {
        super.setUsed(index);
        input1.setUsed(index);
    }
}
