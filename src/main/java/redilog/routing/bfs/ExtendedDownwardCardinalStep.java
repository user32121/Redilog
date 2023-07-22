package redilog.routing.bfs;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;

public class ExtendedDownwardCardinalStep extends CardinalStep {

    @Override
    public Vec3i[] getValidMove(Array3D<BLOCK> grid, Vec3i pos, Vec3i target, Direction direction) {
        Vec3i next = getNextPosition(pos, direction);
        Vec3i nextNext = getNextPosition(next, direction);
        //next cannot already have something there (unless it's the target)
        if (!next.equals(target) && (!grid.isValue(next, BLOCK.AIR) || !grid.isValue(next.add(0, -1, 0), BLOCK.AIR))) {
            return EMPTY_PATH;
        }
        //make sure next not adjacent to other wires
        for (Direction dir : new Direction[] { direction, direction.rotateYClockwise(),
                direction.rotateYCounterclockwise() }) {
            for (int y = -1; y <= 1; ++y) {
                Vec3i adjacent = next.offset(dir).add(0, y, 0);
                if (!adjacent.equals(target) //ok to be adjacent to target
                        && grid.isValue(adjacent, BLOCK.WIRE)) {
                    return EMPTY_PATH;
                }
            }
        }
        //cannot cut wire below
        if (grid.isValue(next.add(0, -2, 0), BLOCK.WIRE)
                && grid.isValue(pos.add(0, -2, 0), BLOCK.WIRE)) {
            return EMPTY_PATH;
        }
        //nextNext not adjacent to wires
        for (Direction dir : new Direction[] { direction, direction.rotateYClockwise(),
                direction.rotateYCounterclockwise() }) {
            for (int y = -1; y <= 1; ++y) {
                Vec3i adjacent = nextNext.offset(dir).add(0, y, 0);
                if (!adjacent.equals(target) //ok to be adjacent to target
                        && grid.isValue(adjacent, BLOCK.WIRE)) {
                    return EMPTY_PATH;
                }
            }
        }
        return new Vec3i[] { next, nextNext };
    }

    @Override
    public Vec3i getNextPosition(Vec3i pos, Direction direction) {
        return super.getNextPosition(pos, direction).add(0, -1, 0);
    }

    @Override
    public int getCost() {
        return 30;
    }
}
