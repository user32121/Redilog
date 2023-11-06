package redilog.synthesis;

import java.util.Collection;
import java.util.function.Supplier;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import redilog.parsing.Expression;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class ConstantNode extends Node {
    public final boolean bit;

    public ConstantNode(Expression owner, boolean bit) {
        super(owner);
        this.bit = bit;
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid) {
        if (bit) {
            grid.set(VecUtil.d2i(getPosition()), BLOCK.BLOCK);
            grid.set(VecUtil.d2i(getPosition()).up(), BLOCK.TORCH);
        }
        outputs.add(new Vec4i(VecUtil.d2i(getPosition()).up(), 13));
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes) {
        //get average positions of inputs and outputs
        Vec3d avg = getPosition();
        int count = 1;
        for (Supplier<Vec3d> pos : outputNodes) {
            avg = avg.add(pos.get());
            ++count;
        }
        avg = avg.multiply(1.0 / count);

        //repel from other nodes
        for (Node n : otherNodes) {
            double distSqr = avg.squaredDistanceTo(n.getPosition());
            avg = avg.lerp(n.getPosition(), -1 / (distSqr + 0.1));
        }

        //clamp by buildspace
        double x = avg.x;
        double y = avg.y;
        double z = avg.z;
        if (x < 0) {
            x = 0;
        } else if (x + 1 >= buildSpace.getXLength()) {
            x = buildSpace.getXLength() - 1;
        }
        if (y < 0) {
            y = 0;
        } else if (y + 2 >= buildSpace.getYLength()) {
            y = buildSpace.getYLength() - 2;
        }
        if (z < 0) {
            z = 0;
        } else if (z + 1 >= buildSpace.getZLength()) {
            z = buildSpace.getZLength() - 1;
        }
        setPosition(new Vec3d(x, y, z));
    }
}
