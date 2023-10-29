package redilog.synthesis;

import redilog.parsing.Expression;

public class ConstantNode extends Node {
    boolean bit;

    public ConstantNode(Expression owner, boolean bit) {
        super(owner);
        this.bit = bit;
    }
}
