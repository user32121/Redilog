package redilog.synthesis;

import redilog.parsing.Expression;

public abstract class Node {
    //TODO store name and other self properties like WireDescriptor?
    public Expression owner;
    public boolean used;

    public Node(Expression owner) {
        this.owner = owner;
    }
}