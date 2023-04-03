package redilog.init;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.Factory;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import redilog.blocks.RedilogPlacerBlock;
import redilog.blocks.RedilogPlacerBlockEntity;

public class RedilogBlocks {

    public static final RedilogPlacerBlock REDILOG_PLACER = register(new RedilogPlacerBlock(
            FabricBlockSettings.of(Material.STONE).requiresTool().strength(-1.0f, 3600000.0f).dropsNothing()),
            "redilog_placer");

    public static final BlockEntityType<RedilogPlacerBlockEntity> REDILOG_PLACER_ENTITY_TYPE = register(
            "redilog_placer_entity", RedilogPlacerBlockEntity::new, REDILOG_PLACER);

    public static void init() {
    }

    private static <T extends Block> T register(T block, String id) {
        Registry.register(Registry.ITEM, new Identifier("redilog", id),
                new BlockItem(block, new FabricItemSettings()));
        return Registry.register(Registry.BLOCK, new Identifier("redilog", id), block);
    }

    private static <T extends BlockEntity> BlockEntityType<T> register(String id, Factory<T> factory, Block... blocks) {
        return Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier("the_fractured_abyss", id),
                FabricBlockEntityTypeBuilder.create(factory, blocks).build());
    }
}
