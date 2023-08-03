package redilog.routing.bfs;

import java.util.List;

import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

/**
 * Determins valid tiles a wire can travel to during routing
 */
public interface BFSStep {

    public static final Vec4i[] EMPTY_PATH = new Vec4i[0];

    public static final BFSStep[] STEPS = {
            new CardinalStep(),
            new VerticalCardinalStep(1),
            new VerticalCardinalStep(-1),
            new ExtendedUpwardCardinalStep(),
            new ExtendedDownwardCardinalStep(),
            //TODO repeaters
    };

    /**
     * @return valid tiles the wire can extend to. Each array contains a grouping that must be placed together
     */
    List<Vec4i[]> getValidMoves(Array3D<BLOCK> grid, Vec4i cur, Vec3i target);

    default int getCost() {
        return 10;
    }
}
