package redilog.routing.steps;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

public class ExtendedUpwardCardinalStep extends CardinalStep {

    public ExtendedUpwardCardinalStep(Direction direction) {
        super(direction);
    }

    @Override
    public Vec4i getValidMove(Array3D<BLOCK> grid, Vec4i pos, Vec3i target, RoutingStep prevStep) {
        Vec4i next = getNextPosition(pos);
        Vec4i nextNext = getNextPosition(next);
        //next cannot already have something there (unless it's the target)
        if (!nextNext.to3i().equals(target) && (!grid.isValue(next.to3i(), BLOCK.AIR)
                || !grid.isValue(next.to3i().down(), BLOCK.AIR)
                || !grid.isValue(nextNext.to3i(), BLOCK.AIR)
                || !grid.isValue(nextNext.to3i().down(), BLOCK.AIR))) {
            return null;
        }
        //make sure next not adjacent to other wires
        for (Direction dir : new Direction[] { direction.rotateYClockwise(), direction.rotateYCounterclockwise() }) {
            for (int y = -1; y <= 1; ++y) {
                Vec3i adjacent = next.to3i().offset(dir).add(0, y, 0);
                if (!adjacent.equals(target) //ok to be adjacent to target
                        && grid.isValue(adjacent, BLOCK.WIRE)) {
                    return null;
                }
            }
        }
        //nextNext not adjacent to wires unless it's target
        if (!nextNext.to3i().equals(target)) {
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
        }
        //cannot cut wire below
        if (grid.isValue(next.to3i().add(0, -2, 0), BLOCK.WIRE)
                && grid.isValue(nextNext.to3i().offset(direction).add(0, -2, 0), BLOCK.WIRE)) {
            return null;
        }
        //must be air above
        if (!grid.isValue(pos.to3i().up(), BLOCK.AIR) || !grid.isValue(next.to3i().up(), BLOCK.AIR)) {
            return null;
        }
        return nextNext;
    }

    @Override
    public Vec4i getNextPosition(Vec4i pos) {
        return super.getNextPosition(pos).add(0, 1, 0, 0);
    }

    @Override
    public int getCost() {
        return 30;
    }

    @Override
    public Vec4i[] place(Vec4i pos, Array3D<BLOCK> grid) {
        Vec4i next = getNextPosition(pos);
        Vec4i nextNext = getNextPosition(next);
        grid.set(next.to3i(), BLOCK.WIRE);
        grid.set(next.to3i().down(), BLOCK.BLOCK);
        grid.set(nextNext.to3i(), BLOCK.WIRE);
        grid.set(nextNext.to3i().down(), BLOCK.BLOCK);
        //make sure not cut by other things
        grid.set(pos.to3i().up(), BLOCK.STRICT_AIR);
        grid.set(next.to3i().up(), BLOCK.STRICT_AIR);
        return new Vec4i[] { next, nextNext };
    }
}
