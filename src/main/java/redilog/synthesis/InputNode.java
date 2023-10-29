package redilog.synthesis;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.util.math.Vec3i;
import redilog.parsing.Expression;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

public class InputNode extends Node {
    public final String name;
    public Vec3i position;
    public Set<Vec4i> outputs = new HashSet<>();

    public InputNode(Expression owner, String name) {
        super(owner);
        this.name = name;
    }

    public boolean isPlaced() {
        return position != null;
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