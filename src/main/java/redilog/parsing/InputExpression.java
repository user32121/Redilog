package redilog.parsing;

import net.minecraft.util.dynamic.Range;

public class InputExpression extends Expression {
    public InputExpression(Range<Integer> range) {
        super(range);
    }

    @Override
    public boolean resolveRange() {
        if (range != null) {
            return true;
        }
        range = new Range<>(0, 0);
        return true;
    }
}