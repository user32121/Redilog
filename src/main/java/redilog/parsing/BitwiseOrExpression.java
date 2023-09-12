package redilog.parsing;

import net.minecraft.util.dynamic.Range;
import redilog.synthesis.Node;
import redilog.synthesis.OrNode;

public class BitwiseOrExpression extends Expression {
    public Expression input1, input2;

    public BitwiseOrExpression(Expression input1, Expression input2) {
        super(null);
        this.input1 = input1;
        this.input2 = input2;
    }

    private static int getMSBNeeded(Range<Integer> r1, Range<Integer> r2) {
        return Math.max(r1.maxInclusive() - r1.minInclusive(),
                r2.maxInclusive() - r2.minInclusive());
    }

    @Override
    public boolean resolveRange() {
        if (range != null) {
            return true;
        }
        if (input1.range == null || input2.range == null) {
            return false;
        }
        range = new Range<>(0, getMSBNeeded(input1.range, input2.range));
        return true;
    }

    @Override
    public Node getNode(int index) throws IndexOutOfBoundsException {
        if (nodes == null) {
            nodes = new Node[range.maxInclusive() - range.minInclusive() + 1];
        }
        if (nodes[index] == null) {
            OrNode node = new OrNode();
            nodes[index] = node;
            node.input1 = input1.getNode(index);
            node.input2 = input2.getNode(index);
        }
        return nodes[index];
    }
}
