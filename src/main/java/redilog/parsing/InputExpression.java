package redilog.parsing;

import net.minecraft.util.dynamic.Range;
import redilog.synthesis.InputNode;
import redilog.synthesis.Node;

public class InputExpression extends Expression {
    public final String name;

    public InputExpression(String name, Range<Integer> range) {
        super(range);
        this.name = name;
    }

    @Override
    public boolean resolveRange() {
        if (range != null) {
            return true;
        }
        range = new Range<>(0, 0);
        return true;
    }

    @Override
    public Node getNode(int index) throws IndexOutOfBoundsException {
        if (nodes == null) {
            nodes = new Node[range.maxInclusive() - range.minInclusive() + 1];
            for (int i = 0; i < nodes.length; ++i) {
                nodes[i] = new InputNode(String.format("%s[%s]", name, index + range.minInclusive()));
            }
        }
        return nodes[index];
    }
}