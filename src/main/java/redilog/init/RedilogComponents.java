package redilog.init;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3i;
import redilog.routing.BLOCK;
import redilog.synthesis.nodes.Component;
import redilog.utils.Vec4i;

public class RedilogComponents {
    public static Component OR_GATE;
    public static Component AND_GATE;
    public static Component NOT_GATE;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            OR_GATE = loadComponent(server.getResourceManager(), new Identifier("redilog", "components/or_gate.nbt"));
            AND_GATE = loadComponent(server.getResourceManager(), new Identifier("redilog", "components/and_gate.nbt"));
            NOT_GATE = loadComponent(server.getResourceManager(), new Identifier("redilog", "components/not_gate.nbt"));
        });
    }

    private static Component loadComponent(ResourceManager rm, Identifier id) {
        try {
            NbtCompound nbt = NbtIo.readCompressed(rm.open(id));
            Component c = new Component(readv3i(nbt.get("size")));

            for (NbtElement e : nbt.getList("inputs", NbtElement.INT_ARRAY_TYPE)) {
                c.inputs.add(readv3i(e));
            }
            if (c.inputs.size() == 0) {
                Redilog.LOGGER.warn(String.format("%s has 0 inputs", id));
            }

            for (NbtElement e : nbt.getList("outputs", NbtElement.INT_ARRAY_TYPE)) {
                c.outputs.add(readv4i(e));
            }
            if (c.outputs.size() == 0) {
                Redilog.LOGGER.warn(String.format("%s has 0 outputs", id));
            }

            List<BlockState> states = new ArrayList<>();
            for (NbtElement e : nbt.getList("palette", NbtElement.COMPOUND_TYPE)) {
                states.add(NbtHelper.toBlockState((NbtCompound) e));
            }
            boolean nonAir = false;
            for (NbtElement e : nbt.getList("blocks", NbtElement.COMPOUND_TYPE)) {
                NbtCompound e2 = (NbtCompound) e;
                BlockState state = states.get(e2.getInt("state"));
                c.blocks.set(readv3i(e2.get("pos")), BLOCK.fromState(state));
                if (!state.isAir()) {
                    nonAir = true;
                }
            }
            if (!nonAir) {
                Redilog.LOGGER.warn(String.format("%s has only air blocks", id));
            }
            return c;
        } catch (IOException | AssertionError e) {
            Redilog.LOGGER.error(String.format("Unable to load %s", id), e);
        }
        return null;
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
