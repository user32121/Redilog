package redilog.parsing;

import net.minecraft.util.dynamic.Range;

public class OutputSExpression extends SymbolExpression {
    public SymbolExpression value;

    public OutputSExpression(Range<Integer> range) {
        super(range);
    }
}