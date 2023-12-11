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

    public final int value;

    public ConstantExpression(Token declaration, int value) {
        super(declaration);
        this.value = value;
    }

    @Override
    public int resolveRange() {
        int value = this.value;
        int size = 0;
        while (value != 0 && value != -1) {
            // nodes.add(new ConstantNode(this, (value & 1) == 1));
            ++size;
            value >>= 1;
        }
        return size;
    }

    @Override
    public Node getNode(int index) {
        while (nodes.size() <= index) {
            nodes.add(null);
        }
        if (nodes.get(index) == null) {
            nodes.set(index, new ConstantNode(this, (value & (1 << index)) != 0));
        }
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

    @Override
    public int evaluateAsConstant() throws RedilogParsingException {
        return value;
    }
}
