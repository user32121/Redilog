package redilog.routing.steps;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

public class CardinalStep implements RoutingStep {
    protected final Direction direction;

    public CardinalStep(Direction direction) {
        this.direction = direction;
    }

    @Override
    public Vec4i getValidMove(Array3D<BLOCK> grid, Vec4i pos, Vec3i target, RoutingStep prevStep) {
        //cannot reverse direction
        if (prevStep instanceof CardinalStep cs && cs.direction == direction.getOpposite()) {
            return null;
        }
        Vec4i next = getNextPosition(pos);
        //reached target
        if (next.to3i().equals(target)) {
            return next;
        }
        //next cannot already have something there (unless it's the target)
        if ((!grid.isValue(next.to3i(), BLOCK.AIR) || !grid.isValue(next.to3i().down(), BLOCK.AIR))) {
            return null;
        }
        //make sure not adjacent to other wires
        for (Direction dir : new Direction[] { direction, direction.rotateYClockwise(),
                direction.rotateYCounterclockwise() }) {
            for (int y = -1; y <= 1; ++y) {
                Vec3i adjacent = next.to3i().offset(dir).add(0, y, 0);
                if (!adjacent.equals(target) //ok to be adjacent to target
                        && grid.isValue(adjacent, BLOCK.WIRE)) {
                    return null;
                }
            }
        }
        return next;
    }

    /**
     * @param pos the current position
     * @return the next position to explore
     */
    public Vec4i getNextPosition(Vec4i pos) {
        return pos.offset(direction).add(0, 0, 0, -1);
    }

    @Override
    public Vec4i[] place(Vec4i pos, Array3D<BLOCK> grid) {
        Vec4i next = getNextPosition(pos);
        grid.set(next.to3i(), BLOCK.WIRE);
        grid.set(next.to3i().down(), BLOCK.BLOCK);
        return new Vec4i[] { next };
    }
}
