package redilog.routing.bfs;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

//TODO wire still getting cut; prevent step from reversing (e.g. south right after north)
public class VerticalCardinalStep extends CardinalStep {
    private int verticalOffset;

    public VerticalCardinalStep(int verticalOffset, Direction direction) {
        super(direction);
        this.verticalOffset = verticalOffset;
    }

    @Override
    public Vec4i getNextPosition(Vec4i pos) {
        return super.getNextPosition(pos).add(0, verticalOffset, 0, 0);
    }

    @Override
    public int getCost() {
        return 20;
    }

    @Override
    public Vec4i getValidMove(Array3D<BLOCK> grid, Vec4i pos, Vec3i target) {
        //make sure wire not cut
        Vec4i next = getNextPosition(pos);
        Vec4i lower = next.getY() < pos.getY() ? next : pos;
        if (!grid.isValue(lower.to3i().up(), BLOCK.AIR)) {
            return null;
        }
        return super.getValidMove(grid, pos, target);
    }

    @Override
    public Vec4i[] place(Vec4i pos, Array3D<BLOCK> grid) {
        Vec4i next = getNextPosition(pos);
        Vec4i lower = next.getY() < pos.getY() ? next : pos;
        grid.set(lower.to3i().up(), BLOCK.STRICT_AIR);
        return super.place(pos, grid);
    }

}
