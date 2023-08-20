package redilog.routing.bfs;

import net.minecraft.util.math.Direction;
import redilog.utils.Vec4i;

public class VerticalCardinalStep extends CardinalStep {
    private int verticalOffset;

    public VerticalCardinalStep(int verticalOffset, Direction direction) {
        super(direction);
        this.verticalOffset = verticalOffset;
    }

    @Override
    public Vec4i getNextPosition(Vec4i pos, Direction direction) {
        return super.getNextPosition(pos, direction).add(0, verticalOffset, 0, 0);
    }

    @Override
    public int getCost() {
        return 20;
    }

}
