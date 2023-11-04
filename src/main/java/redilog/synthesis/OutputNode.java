package redilog.synthesis;

import java.util.Collection;
import java.util.Set;

import net.minecraft.util.math.Box;
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
    public Vec3i position;

    public OutputNode(Expression owner, String name, Node value) {
        super(owner);
        this.name = name;
        this.used = true;
        this.value = value;
        if (value != null) {
            value.outputNodes.add(this);
        }
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid) {
        grid.set(position, BLOCK.WIRE);
        grid.set(position.add(0, -1, 0), BLOCK.BLOCK);
    }

    @Override
    public Set<Vec4i> getOutputs() throws RedilogPlacementException {
        throw new RedilogPlacementException("Cannot use output node as intermediate node");
    }

    @Override
    public Set<Node> getOutputNodes() throws RedilogPlacementException {
        throw new RedilogPlacementException("Cannot use output node as intermediate node");
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes) {
        potentialPosition = VecUtil.i2d(position);
    }
}