package redilog.synthesis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import redilog.parsing.Expression;
import redilog.routing.Placer.BLOCK;
import redilog.routing.RedilogPlacementException;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

//TODO make folder for nodes
public abstract class Node {
    public final Expression owner;
    public boolean used;
    public final Set<Vec4i> outputs = new HashSet<>();
    public final Set<Supplier<Vec3d>> outputNodes = new HashSet<>();
    protected Vec3d position;

    public Node(Expression owner) {
        this.owner = owner;
    }

    /**
     * Acts similarly to a setter but may differ if the node does not support position adjustment
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
     * @param rng
     */
    public abstract void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes, Random rng);

    public abstract void routeBFS(TriConsumer<Set<Vec4i>, Vec4i, Node> bfs) throws RedilogPlacementException;
}