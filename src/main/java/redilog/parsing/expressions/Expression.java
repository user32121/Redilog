package redilog.parsing.expressions;

import java.util.ArrayList;
import java.util.List;

import redilog.parsing.RedilogParsingException;
import redilog.parsing.Token;
import redilog.routing.RedilogPlacementException;
import redilog.synthesis.RedilogSynthesisException;
import redilog.synthesis.nodes.Node;

public abstract class Expression {
    public Token declaration;

    protected List<Node> nodes = new ArrayList<>();

    public Expression(Token declaration) {
        this.declaration = declaration;
    }

    /**
     * Sets the range to the size needed to store data from inputs. 
     * Also recursively calls {@link #resolveRange} on any input expressions.
     * @return the number of nodes this expression expects to use
     */
    public abstract int resolveRange() throws RedilogSynthesisException;

    /**
     * Gets the node at {@code index}. If it is not yet initilized, it should be initialized here.
     * If {@code index >= nodes.size()}, returns a node that would logically be at that index
     * (e.g. a ConstantExpression would return the node corresponding to the 2's complement expansion)
     * @throws IndexOutOfBoundsException if {@code index < 0}
     */
    public abstract Node getNode(int index);

    public void setUsed(int index) {
        getNode(index).used = true;
    }

    public abstract void setInput(Expression expression) throws RedilogParsingException;

    /**
     * Gets all nodes in this expresion tree. This includes all nodes in this Expression and also nodes from any inputs.
     * @throws RedilogPlacementException
     */
    public abstract Iterable<Node> getAllNodes();

    public String nodeAsString(Node node) {
        for (int i = 0; i < nodes.size(); ++i) {
            if (nodes.get(i) == node) {
                return String.format("%s[%d]", declaration, i);
            }
        }
        throw new RuntimeException(
                String.format("Node %s declares owner %s %s but not found in owner.nodes %s", node, this, declaration,
                        nodes));
    }

    /**
     * Gets the value of this expression as if it were a constant expression
     * @throws RedilogParsingException if value of the expression depends on non constants
     */
    public abstract int evaluateAsConstant() throws RedilogParsingException;
}