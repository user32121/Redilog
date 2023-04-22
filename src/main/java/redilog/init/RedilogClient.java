package redilog.init;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import redilog.blocks.BuilderScreen;
import redilog.blocks.LayoutMarkerBlockEntityRenderer;

public class RedilogClient implements ClientModInitializer {
    public static final EntityModelLayer BEAM = new EntityModelLayer(
            new Identifier("redilog", "layout_marker"), "beam");

    @Override
    public void onInitializeClient() {
        HandledScreens.register(Redilog.BUILDER_SCREEN_HANDLER, BuilderScreen::new);

        BlockEntityRendererFactories.register(RedilogBlocks.MARKER_ENTITY, LayoutMarkerBlockEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(BEAM, LayoutMarkerBlockEntityRenderer::createModelData);

        ClientSpriteRegistryCallback.event(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
                .register(((atlasTexture, registry) -> {
                    registry.register(new Identifier("redilog", "block/layout_marker_beam1"));
                    registry.register(new Identifier("redilog", "block/layout_marker_beam2"));
                    registry.register(new Identifier("redilog", "block/layout_marker_beam3"));
                }));
    }
}
