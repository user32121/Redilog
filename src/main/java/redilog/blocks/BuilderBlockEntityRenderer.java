package redilog.blocks;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import redilog.init.RedilogClient;

public class BuilderBlockEntityRenderer implements BlockEntityRenderer<BuilderBlockEntity> {

    //beam consts
    private static final float B_SIZE = 2;
    private static final float B_POS = (16 - B_SIZE) / 2;

    private final ModelPart beam;

    public BuilderBlockEntityRenderer(Context context) {
        beam = context.getLayerModelPart(RedilogClient.BEAM);
        beam.pivotX = beam.pivotY = beam.pivotZ = B_POS;
    }

    @Override
    public boolean rendersOutsideBoundingBox(BuilderBlockEntity blockEntity) {
        return blockEntity.getBuildSpace() != null && blockEntity.getBuildSpace().getAverageSideLength() != 0;
    }

    @Override
    public int getRenderDistance() {
        return 128;
    }

    @Override
    public void render(BuilderBlockEntity entity, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, int overlay) {
        //bounding box (only one block needs to render it, so choose "lowest")
        SpriteIdentifier spriteIdentifier = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
                new Identifier("redilog", "block/layout_beam4"));
        VertexConsumer vertexConsumer = spriteIdentifier.getVertexConsumer(vertexConsumers,
                RenderLayer::getEntityCutout);
        light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        Box box = entity.getBuildSpace();

        //set origin to bottom left of bounding box
        matrices.push();
        matrices.translate(box.minX - entity.getPos().getX(), box.minY - entity.getPos().getY(),
                box.minZ - entity.getPos().getZ());
        //this corresponds to the 4 corners of each face, and we draw a beam along the third direction
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                beam.xScale = 16 / B_SIZE * ((float) box.getXLength() - 1) + 1;
                beam.pivotY = B_POS + 16 * ((float) box.getYLength() - 1) * i;
                beam.pivotZ = B_POS + 16 * ((float) box.getZLength() - 1) * j;
                beam.render(matrices, vertexConsumer, light, overlay);
                beam.pivotY = B_POS;
                beam.xScale = 1;

                beam.pivotX = B_POS + 16 * ((float) box.getXLength() - 1) * i;
                beam.yScale = 16 / B_SIZE * ((float) box.getYLength() - 1) + 1;
                beam.render(matrices, vertexConsumer, light, overlay);
                beam.yScale = 1;
                beam.pivotZ = B_POS;

                beam.pivotY = B_POS + 16 * ((float) box.getYLength() - 1) * j;
                beam.zScale = 16 / B_SIZE * ((float) box.getZLength() - 1) + 1;
                beam.render(matrices, vertexConsumer, light, overlay);
                beam.zScale = 1;
                beam.pivotX = B_POS;
                beam.pivotY = B_POS;
            }
        }
        matrices.pop();
    }
}
