package redilog.routing.steps;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

/**
 * Determins valid tiles a wire can travel to during routing
 */
public interface RoutingStep {

    public static final RoutingStep[] STEPS = initSTEPS();

    private static RoutingStep[] initSTEPS() {
        List<RoutingStep> steps = new ArrayList<>();
        for (Direction dir : new Direction[] { Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH }) {
            steps.add(new CardinalStep(dir));
            steps.add(new RepeaterCardinalStep(dir));
            steps.add(new VerticalCardinalStep(1, dir));
            steps.add(new VerticalCardinalStep(-1, dir));
            steps.add(new ExtendedUpwardCardinalStep(dir));
        }
        return steps.toArray(RoutingStep[]::new);
    }

    /**
     * @return a tile the wire can extend to
     */
    @Nullable
    Vec4i getValidMove(Array3D<BLOCK> grid, Vec4i cur, Vec3i target);

    /**
     * @param pos the position to place from
     * @param grid the grid to place on
     * @return array of positions where wires were placed
     */
    Vec4i[] place(Vec4i pos, Array3D<BLOCK> grid);

    default int getCost() {
        return 10;
    }
}
