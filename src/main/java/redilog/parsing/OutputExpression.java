package redilog.parsing;

import net.minecraft.util.dynamic.Range;

public class OutputExpression extends WireExpression {

    public OutputExpression(Token declaration, String name, Range<Integer> range) {
        super(declaration, name, range);
    }
}