package redilog.synthesis.nodes;

import java.util.List;
import java.util.ArrayList;

import net.minecraft.util.math.Vec3i;
import redilog.routing.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

/**
 * Provides details about a {@link Node}'s component, such as blocks and IO positions
 */
public class Component {
    //TODO margins
    public final Array3D<BLOCK> blocks;
    public final List<Vec3i> inputs = new ArrayList<>();
    public final List<Vec4i> outputs = new ArrayList<>();

    public Component(Vec3i size) {
        this.blocks = new Array3D.Builder<BLOCK>().size(size).build();
    }
}
