package redilog.parsing;

import net.minecraft.util.dynamic.Range;

/**
 * Has a constant numerical value. Constant in this context means it won't change in the circuit,
 * not necessarily during parsing.
 */
public class ConstantExpression extends Expression {
    int value;

    public ConstantExpression(int value) {
        super(new Range<>(0, getMSBNeeded(value)));
        this.value = value;
    }

    private static int getMSBNeeded(int x) {
        int i = 0;
        while (true) {
            ++i;
            x >>= 1;
            if (x == 0) {
                return i;
            } else if (x == -1) {
                return i + 1;
            }
        }
    }

    @Override
    public boolean resolveRange() {
        if (range != null) {
            return true;
        }
        range = new Range<>(0, getMSBNeeded(value));
        return true;
    }
}
