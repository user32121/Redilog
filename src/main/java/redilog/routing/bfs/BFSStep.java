package redilog.routing.bfs;

import java.util.List;

import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;

/**
 * Determins valid tiles a wire can travel to during routing
 */
@FunctionalInterface
public interface BFSStep {

    public static final Vec3i[] EMPTY_PATH = new Vec3i[0];

    public static final BFSStep[] STEPS = {
            //standard horizontal
            new VerticalCardinalStep(0),
            //horizontal up
            new VerticalCardinalStep(1),
            //horizontal down
            new VerticalCardinalStep(-1),
            //TODO optimized horizontal up/down by detecting when a wire can go up or down more tightly around a wire
    };

    /**
     * @return valid tiles the wire can extend to. Each array contains a grouping that must be placed together
     */
    List<Vec3i[]> getValidMoves(Array3D<BLOCK> grid, Vec3i pos, Vec3i target);
}
