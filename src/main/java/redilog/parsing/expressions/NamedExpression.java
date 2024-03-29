package redilog.parsing.expressions;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.dynamic.Range;
import redilog.parsing.Token;
import redilog.synthesis.RedilogSynthesisException;
import redilog.synthesis.nodes.ConstantNode;
import redilog.synthesis.nodes.Node;

/**
 * Represents an expression that has a name and an arbitrary node range
 */
public abstract class NamedExpression extends Expression {
    public final String name;
    public final @Nullable Range<Integer> range;
    public final Node belowRange0 = new ConstantNode(this, false); //used for negative indices

    public NamedExpression(Token declaration, String name, @Nullable Range<Integer> range) {
        super(declaration);
        this.name = name;
        this.range = range;
    }

    @Override
    public int resolveRange() throws RedilogSynthesisException {
        if (range == null) {
            return 1;
        } else {
            return range.maxInclusive() - range.minInclusive() + 1;
        }
    }

    /**
     * Behaves like {@link #getNode}, but the index starts from {@code range.minInclusive()} instead of 0
     */
    public Node getNodeWithAddress(int index) {
        if (range == null) {
            if (index < 0) {
                return belowRange0;
            }
            return getNode(index);
        } else {
            if (index < range.minInclusive()) {
                return belowRange0;
            }
            return getNode(index - range.minInclusive());
        }
    }

    @Override
    public String nodeAsString(Node node) {
        if (node == belowRange0) {
            return String.format("%s[%d]", name, range == null ? -1 : range.minInclusive() - 1);
        }
        if (range != null) {
            for (int i = range.minInclusive(); i <= range.maxInclusive(); ++i) {
                if (getNodeWithAddress(i) == node) {
                    return String.format("%s[%d]", name, i);
                }
            }
        }
        return super.nodeAsString(node);
    }

    public int getRangeMin() {
        return range == null ? 0 : range.minInclusive();
    }
}
