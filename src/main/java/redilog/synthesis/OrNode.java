package redilog.synthesis;

import redilog.parsing.BitwiseOrExpression;

public class OrNode extends Node {
    public Node input1, input2;

    public OrNode(BitwiseOrExpression owner, Node input1, Node input2) {
        super(owner);
        this.input1 = input1;
        this.input2 = input2;
    }
}
