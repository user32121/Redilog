package redilog.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class Redilog implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("redilog");
    public static final String MODID = "redilog";

    public static final Block REDILOG_PLACER = register(
            new Block(FabricBlockSettings.of(Material.STONE).requiresTool().strength(-1.0f, 3600000.0f).dropsNothing()),
            "redilog_placer");

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("redilog init");
    }

    private static <T extends Block> T register(T block, String id) {
        Registry.register(Registry.ITEM, new Identifier(MODID, id), new BlockItem(block, new FabricItemSettings()));
        return Registry.register(Registry.BLOCK, new Identifier(MODID, id), block);
    }
}
