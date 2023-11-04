package redilog.synthesis;

import java.util.Collection;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import redilog.parsing.Expression;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class InputNode extends Node {
    public final String name;
    public Vec3i position;

    public InputNode(Expression owner, String name) {
        super(owner);
        this.name = name;
    }

    public boolean isPlaced() {
        return position != null;
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid) {
        grid.set(position.add(0, -1, 0), BLOCK.BLOCK);
        grid.set(position.add(0, -1, 1), BLOCK.BLOCK);
        grid.set(position.add(0, 0, 1), BLOCK.WIRE);
        outputs.add(new Vec4i(position.add(0, 0, 1), 15));
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes) {
        potentialPosition = VecUtil.vec3i2f(position);
    }
}