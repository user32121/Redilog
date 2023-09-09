package redilog.parsing;

import net.minecraft.util.dynamic.Range;

public class OutputExpression extends Expression {
    public Expression value;

    public OutputExpression(Range<Integer> range) {
        super(range);
    }
}