package redilog.parsing.expressions;

import net.minecraft.util.dynamic.Range;
import redilog.parsing.Token;
import redilog.synthesis.nodes.Node;

public class OutputExpression extends WireExpression {

    public OutputExpression(Token declaration, String name, Range<Integer> range) {
        super(declaration, name, range);
    }

    @Override
    public Node getNode(int index) throws IndexOutOfBoundsException {
        Node node = super.getNode(index);
        if (!node.used) {
            node.used = true;
            setUsed(index);
        }
        return node;
    }

    @Override
    public int resolveRange() {
        int range = super.resolveRange();
        while (nodes.size() < range) {
            nodes.add(null);
        }
        return range;
    }
}