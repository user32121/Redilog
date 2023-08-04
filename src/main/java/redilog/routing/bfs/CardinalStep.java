package redilog.routing.bfs;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

public class CardinalStep implements BFSStep {
    public final static Direction[] HORIZONTAL = new Direction[] { Direction.WEST, Direction.EAST, Direction.NORTH,
            Direction.SOUTH };

    final Direction[] directions;

    public CardinalStep() {
        this(HORIZONTAL);
    }

    public CardinalStep(Direction... directions) {
        this.directions = directions;
    }

    @Override
    public final List<StepData[]> getValidMoves(Array3D<BLOCK> grid, Vec4i pos, Vec3i target) {
        List<StepData[]> res = new ArrayList<>();
        for (Direction direction : directions) {
            StepData[] validMove = getValidMove(grid, pos, target, direction);
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
    public StepData[] getValidMove(Array3D<BLOCK> grid, Vec4i pos, Vec3i target, Direction direction) {
        Vec4i next = getNextPosition(pos, direction);
        //next cannot already have something there (unless it's the target)
        if (!next.to3i().equals(target)
                && (!grid.isValue(next.to3i(), BLOCK.AIR) || !grid.isValue(next.to3i().add(0, -1, 0), BLOCK.AIR))) {
            return EMPTY_PATH;
        }
        //make sure not adjacent to other wires
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
        return new StepData[] { new StepData(next, BLOCK.WIRE, getCost()) };
    }

    /**
     * @param pos the current position
     * @param direction the current direction
     * @return the next position to explore
     */
    public Vec4i getNextPosition(Vec4i pos, Direction direction) {
        return pos.offset(direction).add(0, 0, 0, -1);
    }
}
