package redilog.routing;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.util.math.Vec3i;
import redilog.utils.Vec4i;

public class WireDescriptor {
    //the block which receives power
    Vec3i source;

    //set of all wires attached to input
    //reading from any of these should give the same logical value (high/low) given sufficient time
    Set<Vec4i> wires = new HashSet<>();
}
