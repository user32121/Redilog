package redilog.synthesis;

import java.util.Set;

import net.minecraft.util.math.Vec3i;
import redilog.parsing.Expression;
import redilog.routing.Placer.BLOCK;
import redilog.routing.RedilogPlacementException;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

public abstract class Node {
    public Expression owner;
    public boolean used;

    public Node(Expression owner) {
        this.owner = owner;
    }

    public boolean isDebug() {
        return owner.declaration.getValue().contains("DEBUG");
    }

    public abstract boolean isPlaced();

    public abstract void placeAt(Array3D<BLOCK> grid, Vec3i pos);

    public abstract Set<Vec4i> getOutputs() throws RedilogPlacementException;
}