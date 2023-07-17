package redilog.routing;

import java.util.List;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;

/**
 * Determins valid tiles a wire can travel to during routing
 */
@FunctionalInterface
public interface RoutingBFSStep {

    public static final Vec3i[] EMPTY_PATH = new Vec3i[0];

    public static final RoutingBFSStep[] STEPS = {
            //standard horizontal
            new DirectionalRoutingBFSStep() {
                @Override
                public Vec3i[] getValidMove(Array3D<BLOCK> grid, Vec3i pos, Vec3i target, Direction direction) {
                    Vec3i next = pos.offset(direction);
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
            }
            //TODO horizontal up
            //TODO horizontal down
    };

    /**
     * @return valid tiles the wire can extend to. Each array contains a grouping that must be placed together
     */
    List<Vec3i[]> getValidMoves(Array3D<BLOCK> grid, Vec3i pos, Vec3i target);
}
