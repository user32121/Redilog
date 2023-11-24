package redilog.parsing;

import redilog.synthesis.AndNode;
import redilog.synthesis.Node;

public class BitwiseAndExpression extends BitwiseOrExpression {
    public BitwiseAndExpression(Token declaration, Expression input1, Expression input2) {
        super(declaration, input1, input2);
    }

    @Override
    public Node getNode(int index) throws IndexOutOfBoundsException {
        if (nodes == null) {
            nodes = new Node[range.maxInclusive() - range.minInclusive() + 1];
        }
        if (nodes[index] == null) {
            AndNode node = new AndNode(this, input1.getNode(index), input2.getNode(index));
            nodes[index] = node;
        }
        return nodes[index];
    }
}
