package redilog.routing.bfs;

import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

public class RepeaterCardinalStep extends CardinalStep {

    @Override
    public StepData[] getValidMove(Array3D<BLOCK> grid, Vec4i pos, Vec3i target, Direction direction) {
        Vec4i next = getNextPosition(pos, direction);
        next.setW(0); //repeater does not provide power except in output direction
        Vec4i nextNext = getNextPosition(next, direction);
        nextNext.setW(15); //repeater output
        //nextNext cannot already have something there (unless it's the target)
        if (!nextNext.to3i().equals(target) && (!grid.isValue(nextNext.to3i(), BLOCK.AIR)
                || !grid.isValue(nextNext.to3i().add(0, -1, 0), BLOCK.AIR))) {
            return EMPTY_PATH;
        }
        //make sure not adjacent to other wires
        for (Direction dir : new Direction[] { direction, direction.rotateYClockwise(),
                direction.rotateYCounterclockwise() }) {
            for (int y = -1; y <= 1; ++y) {
                Vec3i adjacent = nextNext.to3i().offset(dir).add(0, y, 0);
                if (!adjacent.equals(target) //ok to be adjacent to target
                        && grid.isValue(adjacent, BLOCK.WIRE)) {
                    return EMPTY_PATH;
                }
            }
        }
        return new StepData[] { new StepData(next, switch (direction) {
            case NORTH -> BLOCK.REPEATER_NORTH;
            case SOUTH -> BLOCK.REPEATER_SOUTH;
            case EAST -> BLOCK.REPEATER_EAST;
            case WEST -> BLOCK.REPEATER_WEST;
            default -> throw new NotImplementedException(direction.toString() + "not implemented");
        }), new StepData(nextNext, BLOCK.WIRE) };
    }

    @Override
    public int getCost() {
        return 100;
    }
}
