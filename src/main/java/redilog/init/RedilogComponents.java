package redilog.init;

import java.io.IOException;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import redilog.synthesis.nodes.Component;

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
            return new Component(nbt, id);
        } catch (IOException | AssertionError e) {
            Redilog.LOGGER.error(String.format("Unable to load %s", id), e);
            return null;
        }
    }
}
