package redilog.parsing;

import net.minecraft.util.dynamic.Range;

public class BitwiseOrExpression extends Expression {
    public Expression input1, input2;

    public BitwiseOrExpression(Range<Integer> range) {
        super(range);
    }
}
