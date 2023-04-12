package redilog.synthesis;

import java.util.HashMap;
import java.util.Map;

public class Graph {
    public Map<String, Node> inputs = new HashMap<>();
    public Map<String, Node> outputs = new HashMap<>();
    public Map<String, Node> nodes = new HashMap<>();

    public Map<String, Token> nodeDeclarations = new HashMap<>();
}
