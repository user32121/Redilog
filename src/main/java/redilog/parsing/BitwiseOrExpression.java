package redilog.parsing;

import net.minecraft.util.dynamic.Range;

public class BitwiseOrExpression extends SymbolExpression {
    public SymbolExpression input1, input2;

    public BitwiseOrExpression(Range<Integer> range) {
        super(range);
    }
}
