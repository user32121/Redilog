package redilog.routing;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.util.math.Vec3i;

public class WireDescriptor {
    //the block in the node which receives power
    //this is not necessarily a wire, so do not write over it
    Vec3i input;

    //set of all wires attached to input
    //reading from any of these should give the same value given sufficient time
    Set<Vec3i> wires = new HashSet<>();
}
