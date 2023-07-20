package redilog.routing.bfs;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public class VerticalCardinalStep extends CardinalStep {
    private int verticalOffset;

    public VerticalCardinalStep(int verticalOffset) {
        this.verticalOffset = verticalOffset;
    }

    public Vec3i getNextPosition(Vec3i pos, Direction direction) {
        return pos.offset(direction).add(0, verticalOffset, 0);
    }

}
