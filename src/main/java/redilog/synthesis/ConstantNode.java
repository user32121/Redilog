package redilog.synthesis;

import java.util.Collection;

import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.util.math.Box;
import redilog.parsing.Expression;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;

public class ConstantNode extends Node {
    public final boolean bit;

    public ConstantNode(Expression owner, boolean bit) {
        super(owner);
        this.bit = bit;
    }

    public boolean isPlaced() {
        return !outputs.isEmpty();
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("Unimplemented method 'placeAt'");
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("Unimplemented method 'adjustPotentialPosition'");

    }
}
