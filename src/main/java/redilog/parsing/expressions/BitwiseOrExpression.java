package redilog.parsing.expressions;

import com.google.common.collect.Iterables;

import redilog.parsing.RedilogParsingException;
import redilog.parsing.Token;
import redilog.synthesis.nodes.Node;
import redilog.synthesis.nodes.OrNode;

public class BitwiseOrExpression extends Expression {
    public Expression input1, input2;

    public BitwiseOrExpression(Token declaration, Expression input1, Expression input2) {
        super(declaration);
        this.input1 = input1;
        this.input2 = input2;
    }

    @Override
    public int resolveRange() {
        int range1 = input1.resolveRange();
        int range2 = input2.resolveRange();
        return Math.max(range1, range2);
    }

    @Override
    public Node getNode(int index) {
        while (nodes.size() <= index) {
            nodes.add(null);
        }
        if (nodes.get(index) == null) {
            nodes.set(index, new OrNode(this, input1.getNode(index), input2.getNode(index)));
        }
        return nodes.get(index);
    }

    @Override
    public void setInput(Expression expression) throws RedilogParsingException {
        throw new RedilogParsingException("Ambiguous input assignment to dual input " + getClass());
    }

    @Override
    public void setUsed(int index) {
        super.setUsed(index);
        input1.setUsed(index);
        input2.setUsed(index);
    }

    @Override
    public Iterable<Node> getAllNodes() {
        for (int i = 0; i < nodes.size(); ++i) {
            getNode(i);
        }
        return Iterables.concat(nodes, input1.getAllNodes(), input2.getAllNodes());
    }
}
