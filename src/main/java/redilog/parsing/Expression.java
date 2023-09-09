package redilog.parsing;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.dynamic.Range;

public abstract class Expression {
    @Nullable
    public Range<Integer> range;

    public Expression(Range<Integer> range) {
        this.range = range;
    }
}