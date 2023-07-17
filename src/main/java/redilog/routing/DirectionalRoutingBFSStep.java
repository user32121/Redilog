package redilog.routing;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;

public abstract class DirectionalRoutingBFSStep implements RoutingBFSStep {
    public final static Direction[] HORIZONTAL = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST,
            Direction.EAST };

    final Direction[] directions;

    public DirectionalRoutingBFSStep() {
        this(HORIZONTAL);
    }

    public DirectionalRoutingBFSStep(Direction... directions) {
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
    public abstract Vec3i[] getValidMove(Array3D<BLOCK> grid, Vec3i pos, Vec3i target, Direction direction);
}
