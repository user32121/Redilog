package redilog.parsing.expressions;

import net.minecraft.util.dynamic.Range;
import redilog.parsing.Token;
import redilog.synthesis.nodes.Node;
import redilog.synthesis.nodes.OutputNode;

public class OutputExpression extends WireExpression {

    public OutputExpression(Token declaration, String name, Range<Integer> range) {
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
            OutputNode node = new OutputNode(this, String.format("%s[%s]", name, index + getRangeMin()), nodeValue);
            nodes.set(index, node);
        }
        if (!nodes.get(index).used) {
            nodes.get(index).used = true;
            setUsed(index);
        }
        return nodes.get(index);
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