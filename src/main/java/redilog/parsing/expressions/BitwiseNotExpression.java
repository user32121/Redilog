package redilog.parsing.expressions;

import com.google.common.collect.Iterables;

import redilog.parsing.RedilogParsingException;
import redilog.parsing.Token;
import redilog.synthesis.RedilogSynthesisException;
import redilog.synthesis.nodes.Node;
import redilog.synthesis.nodes.NotNode;

public class BitwiseNotExpression extends Expression {
    public Expression input1;

    public BitwiseNotExpression(Token declaration, Expression input1) {
        super(declaration);
        this.input1 = input1;
    }

    @Override
    public int resolveRange() throws RedilogSynthesisException {
        int range1 = input1.resolveRange();
        return range1;
    }

    @Override
    public Node getNode(int index) {
        while (nodes.size() <= index) {
            nodes.add(null);
        }
        if (nodes.get(index) == null) {
            nodes.set(index, new NotNode(this, input1.getNode(index)));
        }
        return nodes.get(index);
    }

    @Override
    public void setInput(Expression expression) throws RedilogParsingException {
        input1 = expression;
    }

    @Override
    public void setUsed(int index) {
        super.setUsed(index);
        input1.setUsed(index);
    }

    @Override
    public Iterable<Node> getAllNodes() {
        for (int i = 0; i < nodes.size(); ++i) {
            getNode(i);
        }
        return Iterables.concat(nodes, input1.getAllNodes());
    }

    @Override
    public int evaluateAsConstant() throws RedilogParsingException {
        return ~input1.evaluateAsConstant();
    }
}
