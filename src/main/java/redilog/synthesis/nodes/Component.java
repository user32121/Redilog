package redilog.synthesis.nodes;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.util.math.Vec3i;
import redilog.routing.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

/**
 * Provides details about a {@link Node}'s component, such as blocks and IO positions
 */
public class Component {
    public final Array3D<BLOCK> blocks;
    public final Set<Vec3i> inputs = new HashSet<>();
    public final Set<Vec4i> outputs = new HashSet<>();

    public Component(Vec3i size) {
        this.blocks = new Array3D.Builder<BLOCK>().size(size).build();
    }
}
