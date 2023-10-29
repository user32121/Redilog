package redilog.synthesis;

import redilog.parsing.Expression;

public class InputNode extends Node {
    public final String name;

    public InputNode(Expression owner, String name) {
        super(owner);
        this.name = name;
    }
}