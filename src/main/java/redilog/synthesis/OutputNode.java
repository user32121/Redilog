package redilog.synthesis;

import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.util.math.Vec3i;
import redilog.parsing.Expression;
import redilog.routing.RedilogPlacementException;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

public class OutputNode extends Node {
    public Node value;
    public final String name;
    public Vec3i input;

    public OutputNode(Expression owner, String name) {
        super(owner);
        this.name = name;
        this.used = true;
    }

    public boolean isPlaced() {
        return input != null;
    }

    @Override
    public void placeAt(Array3D<BLOCK> grid, Vec3i pos) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("Unimplemented method 'placeAt'");
    }

    @Override
    public Set<Vec4i> getOutputs() throws RedilogPlacementException {
        throw new RedilogPlacementException("Cannot use output as intermediate node");
    }
}