package redilog.synthesis;

import redilog.parsing.Expression;

public class OutputNode extends Node {
    public Node value;
    public final String name;

    public OutputNode(Expression owner, String name) {
        super(owner);
        this.name = name;
        this.used = true;
    }
}