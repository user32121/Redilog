package redilog.parsing;

import net.minecraft.util.dynamic.Range;

public class WireExpression extends Expression {
    public Expression value;

    public WireExpression(Range<Integer> range) {
        super(range);
    }

    @Override
    public boolean resolveRange() {
        if (range != null) {
            return true;
        }
        if (value == null) {
            range = new Range<Integer>(0, 0);
            return true;
        }
        range = value.range;
        return range != null;
    }
}
