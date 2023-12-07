package redilog.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import redilog.blocks.BuilderScreenHandler;

public class Redilog implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("redilog");

    public static final ExtendedScreenHandlerType<BuilderScreenHandler> BUILDER_SCREEN_HANDLER = Registry
            .register(Registry.SCREEN_HANDLER, new Identifier("redilog", "builder"),
                    new ExtendedScreenHandlerType<>(BuilderScreenHandler::new));

    public static final Identifier BUILDER_SYNC_PACKET = new Identifier("redilog", "builder_sync");

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("redilog init");

        RedilogBlocks.init();
        RedilogGamerules.init();
        RedilogComponents.init();
    }
}
