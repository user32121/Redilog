package redilog.synthesis;

public class OutputNode extends Node {
    public Node value;
    public final String name;

    public OutputNode(String name) {
        this.name = name;
    }
}