package redilog.routing.steps;

import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import redilog.routing.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

public class RepeaterCardinalStep extends CardinalStep {

    public RepeaterCardinalStep(Direction direction) {
        super(direction);
    }

    @Override
    public Vec4i getValidMove(Array3D<BLOCK> grid, Vec4i pos, Vec3i target, RoutingStep prevStep) {
        Vec4i next = getNextPosition(pos);
        next.setW(0); //repeater does not provide power except in output direction
        Vec4i nextNext = getNextPosition(next);
        nextNext.setW(15); //repeater output
        //nextNext cannot already have something there (unless it's the target)
        if (!nextNext.to3i().equals(target) && (!grid.isValue(next.to3i(), BLOCK.AIR)
                || !grid.isValue(next.to3i().down(), BLOCK.AIR)
                || !grid.isValue(nextNext.to3i(), BLOCK.AIR)
                || !grid.isValue(nextNext.to3i().down(), BLOCK.AIR))) {
            return null;
        }
        //reached target
        if (nextNext.to3i().equals(target)) {
            return nextNext;
        }
        //make sure not adjacent to other wires
        for (Direction dir : new Direction[] { direction, direction.rotateYClockwise(),
                direction.rotateYCounterclockwise() }) {
            for (int y = -1; y <= 1; ++y) {
                Vec3i adjacent = nextNext.to3i().offset(dir).add(0, y, 0);
                if (!adjacent.equals(target) //ok to be adjacent to target
                        && grid.isValue(adjacent, BLOCK.WIRE)) {
                    return null;
                }
            }
        }
        return nextNext;
    }

    @Override
    public int getCost() {
        return 100;
    }

    @Override
    public Vec4i[] place(Vec4i pos, Array3D<BLOCK> grid) {
        Vec4i next = getNextPosition(pos);
        next.setW(0);
        Vec4i nextNext = getNextPosition(next);
        nextNext.setW(15);
        grid.set(next.to3i(), switch (direction) {
            case NORTH -> BLOCK.REPEATER_NORTH;
            case SOUTH -> BLOCK.REPEATER_SOUTH;
            case EAST -> BLOCK.REPEATER_EAST;
            case WEST -> BLOCK.REPEATER_WEST;
            default -> throw new NotImplementedException(direction + " not implemented");
        });
        grid.set(next.to3i().down(), BLOCK.BLOCK);
        grid.set(nextNext.to3i(), BLOCK.WIRE);
        grid.set(nextNext.to3i().down(), BLOCK.BLOCK);
        return new Vec4i[] { next, nextNext };
    }
}
