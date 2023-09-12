package redilog.parsing;

import net.minecraft.util.dynamic.Range;
import redilog.synthesis.Node;
import redilog.synthesis.OutputNode;

public class WireExpression extends Expression {
    public final String name;
    public Expression value;

    public WireExpression(String name, Range<Integer> range) {
        super(range);
        this.name = name;
    }

    @Override
    public boolean resolveRange() {
        if (range != null) {
            return true;
        }
        if (value == null) {
            range = new Range<Integer>(0, 0);
            return true;
        }
        range = value.range;
        return range != null;
    }

    @Override
    public Node getNode(int index) throws IndexOutOfBoundsException {
        if (nodes == null) {
            nodes = new Node[range.maxInclusive() - range.minInclusive() + 1];
        }
        if (nodes[index] == null) {
            OutputNode node = new OutputNode(String.format("%s[%s]", name, index + range.minInclusive()));
            nodes[index] = node;
            node.value = value.getNode(index);
        }
        return nodes[index];
    }
}
