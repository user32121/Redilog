package redilog.synthesis;

import java.util.Collection;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import redilog.parsing.Expression;
import redilog.routing.Placer.BLOCK;
import redilog.routing.RedilogPlacementException;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class OutputNode extends Node {
    public final Node value;
    public final String name;

    public OutputNode(Expression owner, String name, Node value) {
        super(owner);
        this.name = name;
        this.used = true;
        this.value = value;
        if (value != null) {
            value.outputNodes.add(() -> getPosition());
        }
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid) {
        grid.set(VecUtil.d2i(getPosition()), BLOCK.WIRE);
        grid.set(VecUtil.d2i(getPosition().add(0, -1, 0)), BLOCK.BLOCK);
    }

    @Override
    public Set<Vec4i> getOutputs() throws RedilogPlacementException {
        throw new RedilogPlacementException("Cannot use output node as intermediate node");
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes, Random rng) {
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

    @Override
    public void routeBFS(TriConsumer<Set<Vec4i>, Vec3i, Node> bfs) throws RedilogPlacementException {
        if (value != null) {
            bfs.accept(value.getOutputs(), VecUtil.d2i(position), value);
        }
    }
}