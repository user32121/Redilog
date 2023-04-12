package redilog.synthesis;

import java.util.Optional;

import net.minecraft.util.dynamic.Range;

public class Node implements Expression {
    public Optional<Range<Integer>> range;
    public Expression value;

    public Node(Optional<Range<Integer>> range) {
        this.range = range;
    }
}
