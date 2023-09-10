package redilog.parsing;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.dynamic.Range;

public abstract class Expression {
    @Nullable
    public Range<Integer> range;

    public Expression(Range<Integer> range) {
        this.range = range;
    }

    /**
     * Sets the range to the size needed to store data from any input
     * @return true if the function succeeded
     */
    public abstract boolean resolveRange();
}