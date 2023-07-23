package redilog.routing.bfs;

import java.util.List;

import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;

/**
 * Determins valid tiles a wire can travel to during routing
 */
public interface BFSStep {

    public static final Vec3i[] EMPTY_PATH = new Vec3i[0];

    public static final BFSStep[] STEPS = {
            new CardinalStep(),
            new VerticalCardinalStep(1),
            new VerticalCardinalStep(-1),
            new ExtendedUpwardCardinalStep(),
            new ExtendedDownwardCardinalStep(),
    };

    /**
     * @return valid tiles the wire can extend to. Each array contains a grouping that must be placed together
     */
    List<Vec3i[]> getValidMoves(Array3D<BLOCK> grid, Vec3i pos, Vec3i target);

    default int getCost() {
        return 10;
    }
}