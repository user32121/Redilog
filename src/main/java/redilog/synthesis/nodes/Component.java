package redilog.synthesis.nodes;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3i;
import redilog.init.Redilog;
import redilog.routing.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

/**
 * Provides details about a {@link Node}'s component, such as blocks and IO positions
 */
public class Component {
    public final Array3D<BLOCK> blocks;
    public final List<Vec3i> inputs = new ArrayList<>();
    public final List<Vec4i> outputs = new ArrayList<>();
    public Vec3i margin; //minimum number of blocks between components

    public Component(Vec3i size) {
        this.blocks = new Array3D.Builder<BLOCK>().size(size).build();
    }

    /**
     * load a component from nbt
     * @param nbt data to load from, usually pulled directly from an nbt file (e.g. {@code NbtIo.readCompressed(rm.open(id));})
     * @param id id to use in warnings
     */
    public Component(NbtCompound nbt, Identifier id) {
        this(readv3i(nbt.get("size")));

        for (NbtElement e : nbt.getList("inputs", NbtElement.INT_ARRAY_TYPE)) {
            inputs.add(readv3i(e));
        }
        if (inputs.size() == 0) {
            Redilog.LOGGER.warn(String.format("%s has 0 inputs", id));
        }

        for (NbtElement e : nbt.getList("outputs", NbtElement.INT_ARRAY_TYPE)) {
            outputs.add(readv4i(e));
        }
        if (outputs.size() == 0) {
            Redilog.LOGGER.warn(String.format("%s has 0 outputs", id));
        }

        margin = readv3i(nbt.get("margin"));

        List<BlockState> states = new ArrayList<>();
        for (NbtElement e : nbt.getList("palette", NbtElement.COMPOUND_TYPE)) {
            states.add(NbtHelper.toBlockState((NbtCompound) e));
        }
        boolean nonAir = false;
        for (NbtElement e : nbt.getList("blocks", NbtElement.COMPOUND_TYPE)) {
            NbtCompound e2 = (NbtCompound) e;
            BlockState state = states.get(e2.getInt("state"));
            blocks.set(readv3i(e2.get("pos")), BLOCK.fromState(state));
            if (!state.isAir()) {
                nonAir = true;
            }
        }
        if (!nonAir) {
            Redilog.LOGGER.warn(String.format("%s has only air blocks", id));
        }
    }

    private static Vec3i readv3i(NbtElement nbt) {
        int[] ar = null;
        if (nbt instanceof NbtIntArray nbtia) {
            ar = nbtia.getIntArray();
        }
        if (nbt instanceof NbtList nbtl) {
            ar = new int[nbtl.size()];
            for (int i = 0; i < nbtl.size(); ++i) {
                ar[i] = nbtl.getInt(i);
            }
        }
        if (ar == null) {
            throw new AssertionError(String.format("%s is not an NbtIntArray nor an NbtList of NbtInt", nbt));
        }
        if (ar.length != 3) {
            throw new AssertionError(String.format("%s has %s elements (expected 3)", nbt, ar.length));
        }
        return new Vec3i(ar[0], ar[1], ar[2]);
    }

    private static Vec4i readv4i(NbtElement nbt) {
        int[] ar = null;
        if (nbt instanceof NbtIntArray nbtia) {
            ar = nbtia.getIntArray();
        }
        if (nbt instanceof NbtList nbtl) {
            ar = new int[nbtl.size()];
            for (int i = 0; i < nbtl.size(); ++i) {
                ar[i] = nbtl.getInt(i);
            }
        }
        if (ar == null) {
            throw new AssertionError(String.format("%s is not an NbtIntArray nor an NbtList of NbtInt", nbt));
        }
        if (ar.length != 4) {
            throw new AssertionError(String.format("%s has %s elements (expected 4)", nbt, ar.length));
        }
        return new Vec4i(ar[0], ar[1], ar[2], ar[3]);
    }
}
