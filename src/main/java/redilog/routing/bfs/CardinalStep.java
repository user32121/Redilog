package redilog.routing.bfs;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;

public class CardinalStep implements BFSStep {
    public final static Direction[] HORIZONTAL = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST,
            Direction.EAST };

    final Direction[] directions;

    public CardinalStep() {
        this(HORIZONTAL);
    }

    public CardinalStep(Direction... directions) {
        this.directions = directions;
    }

    @Override
    public List<Vec3i[]> getValidMoves(Array3D<BLOCK> grid, Vec3i pos, Vec3i target) {
        List<Vec3i[]> res = new ArrayList<>();
        for (Direction direction : directions) {
            Vec3i[] validMove = getValidMove(grid, pos, target, direction);
            if (validMove.length > 0) {
                res.add(validMove);
            }
        }
        return res;
    }

    /**
     * @param direction the direction to check
     * @return a valid path the wire can extend to. An empty array means no valid path could be found.
     */
    public Vec3i[] getValidMove(Array3D<BLOCK> grid, Vec3i pos, Vec3i target, Direction direction) {
        Vec3i next = getNextPosition(pos, direction);
        //check if already at target
        if (next.equals(target)) {
            return new Vec3i[] { target };
        }
        //next cannot already have something there
        if (!grid.isValue(next, BLOCK.AIR) || !grid.isValue(next.add(0, -1, 0), BLOCK.AIR)) {
            return EMPTY_PATH;
        }
        //make sure not adjacent to other wires
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
        return new Vec3i[] { next };
    }

    /**
     * @param pos the current position
     * @param direction the current direction
     * @return the next position to explore
     */
    public Vec3i getNextPosition(Vec3i pos, Direction direction) {
        return pos.offset(direction);
    }
}
