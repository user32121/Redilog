package redilog.routing.bfs;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

public class ExtendedDownwardCardinalStep extends CardinalStep {

    @Override
    public StepData[] getValidMove(Array3D<BLOCK> grid, Vec4i pos, Vec3i target, Direction direction) {
        Vec4i next = getNextPosition(pos, direction);
        Vec4i nextNext = getNextPosition(next, direction);
        //next cannot already have something there (unless it's the target)
        if (!next.to3i().equals(target)
                && (!grid.isValue(next.to3i(), BLOCK.AIR) || !grid.isValue(next.to3i().add(0, -1, 0), BLOCK.AIR)
                        || !grid.isValue(nextNext.to3i(), BLOCK.AIR)
                        || !grid.isValue(nextNext.to3i().add(0, -1, 0), BLOCK.AIR))) {
            return EMPTY_PATH;
        }
        //make sure next not adjacent to other wires
        for (Direction dir : new Direction[] { direction, direction.rotateYClockwise(),
                direction.rotateYCounterclockwise() }) {
            for (int y = -1; y <= 1; ++y) {
                Vec3i adjacent = next.to3i().offset(dir).add(0, y, 0);
                if (!adjacent.equals(target) //ok to be adjacent to target
                        && grid.isValue(adjacent, BLOCK.WIRE)) {
                    return EMPTY_PATH;
                }
            }
        }
        //cannot cut wire below
        if (grid.isValue(next.to3i().add(0, -2, 0), BLOCK.WIRE)
                && grid.isValue(pos.to3i().add(0, -2, 0), BLOCK.WIRE)) {
            return EMPTY_PATH;
        }
        //nextNext not adjacent to wires
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
        return new StepData[] { new StepData(next, BLOCK.WIRE, getCost()),
                new StepData(nextNext, BLOCK.WIRE, getCost()) };
    }

    @Override
    public Vec4i getNextPosition(Vec4i pos, Direction direction) {
        return super.getNextPosition(pos, direction).add(0, -1, 0, 0);
    }

    @Override
    public int getCost() {
        return 30;
    }
}
