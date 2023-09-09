package redilog.synthesis;

import net.minecraft.util.dynamic.Range;
import redilog.synthesis.SymbolGraph.Expression;

public class BitwiseOrExpression extends Expression {
    public Expression input1, input2;

    public BitwiseOrExpression(Range<Integer> range) {
        super(range);
    }
}
