package redilog.parsing.expressions;

import redilog.parsing.RedilogParsingException;
import redilog.parsing.Token;
import redilog.synthesis.nodes.ConstantNode;
import redilog.synthesis.nodes.Node;

/**
 * Has a constant numerical value. Constant in this context means it won't change in the circuit,
 * not necessarily during parsing.
 */
public class ConstantExpression extends Expression {

    public ConstantExpression(Token declaration, int value) {
        super(declaration);
        while (value != 0 && value != -1) {
            nodes.add(new ConstantNode(this, (value & 1) == 1));
            value >>= 1;
        }
    }

    @Override
    public int resolveRange() {
        return nodes.size();
    }

    @Override
    public Node getNode(int index) {
        return nodes.get(index);
    }

    @Override
    public void setInput(Expression expression) throws RedilogParsingException {
        throw new RedilogParsingException(getClass() + " cannot be assigned an expression");
    }

    @Override
    public Iterable<Node> getAllNodes() {
        return nodes;
    }
}
