package redilog.synthesis;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.util.math.Vec3i;
import redilog.parsing.Expression;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

public class ConstantNode extends Node {
    boolean bit;
    Set<Vec4i> outputs = new HashSet<>();

    public ConstantNode(Expression owner, boolean bit) {
        super(owner);
        this.bit = bit;
    }

    public boolean isPlaced() {
        return !outputs.isEmpty();
    }

    @Override
    public void placeAt(Array3D<BLOCK> grid, Vec3i pos) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'placeAt'");
    }

    @Override
    public Set<Vec4i> getOutputs() {
        return outputs;
    }
}
