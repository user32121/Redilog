package redilog.parsing;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.dynamic.Range;
import redilog.synthesis.Node;

public abstract class Expression {
    @Nullable
    public Range<Integer> range;

    protected Node[] nodes;

    public Expression(Range<Integer> range) {
        this.range = range;
    }

    /**
     * Sets the range to the size needed to store data from any input
     * @return true if the function succeeded
     */
    public abstract boolean resolveRange();

    /**
     * Gets the node at {@code index}. If it is not yet initilized, it should be initialized here.
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= range.maxInclusive() - range.minInclusive() + 1}
     */
    public abstract Node getNode(int index) throws IndexOutOfBoundsException;
}