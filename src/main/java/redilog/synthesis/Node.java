package redilog.synthesis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import redilog.parsing.Expression;
import redilog.routing.Placer.BLOCK;
import redilog.routing.RedilogPlacementException;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

public abstract class Node {
    public final Expression owner;
    public boolean used;
    public final Set<Vec4i> outputs = new HashSet<>();
    public final Set<Supplier<Vec3d>> outputNodes = new HashSet<>();
    private Vec3d position;

    public Node(Expression owner) {
        this.owner = owner;
    }

    public Vec3d getPosition() {
        return position;
    }

    public void setPosition(Vec3d pos) {
        this.position = pos;
    }

    /**
     * Acts similarly to {@link #setPosition} but may differ if the node does not support position adjustment
     */
    public void setPotentialPosition(Vec3d pos) {
        this.position = pos;
    }

    public boolean isDebug() {
        return owner.declaration.getValue().contains("DEBUG");
    }

    public abstract void placeAtPotentialPos(Array3D<BLOCK> grid);

    public Set<Vec4i> getOutputs() throws RedilogPlacementException {
        return outputs;
    }

    /**
     * Move potential position around so that it's close to its inputs and outputs
     * @param buildSpace the space a node must fit inside
     * @param otherNodes reference to other nodes so it doesn't get too close
     */
    public abstract void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes);
}