package redilog.parsing;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.dynamic.Range;

public abstract class SymbolExpression {
    @Nullable
    public Range<Integer> range;

    public SymbolExpression(Range<Integer> range) {
        this.range = range;
    }
}