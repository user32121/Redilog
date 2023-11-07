package redilog.synthesis;

import java.util.Collection;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import redilog.parsing.Expression;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class InputNode extends Node {

    public final String name;

    public InputNode(Expression owner, String name) {
        super(owner);
        this.name = name;
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid) {
        grid.set(VecUtil.d2i(getPosition()).add(0, -1, 0), BLOCK.BLOCK);
        grid.set(VecUtil.d2i(getPosition()).add(0, -1, 1), BLOCK.BLOCK);
        grid.set(VecUtil.d2i(getPosition()).add(0, 0, 1), BLOCK.WIRE);
        outputs.add(new Vec4i(VecUtil.d2i(getPosition()).add(0, 0, 1), 15));
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes) {
        //NO OP
    }

    @Override
    public void setPotentialPosition(Vec3d pos) {
        //NO OP
    }

    public void setPosition(Vec3d pos) {
        position = pos;
    }

    public Vec3d getPosition() {
        return position;
    }
}