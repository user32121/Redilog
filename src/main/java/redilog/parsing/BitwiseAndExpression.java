package redilog.parsing;

import redilog.synthesis.AndNode;
import redilog.synthesis.Node;

public class BitwiseAndExpression extends BitwiseOrExpression {
    public BitwiseAndExpression(Token declaration, Expression input1, Expression input2) {
        super(declaration, input1, input2);
    }

    @Override
    public Node getNode(int index) throws IndexOutOfBoundsException {
        while (nodes.size() <= index) {
            nodes.add(null);
        }
        if (nodes.get(index) == null) {
            nodes.set(index, new AndNode(this, input1.getNode(index), input2.getNode(index)));
        }
        return nodes.get(index);
    }
}
