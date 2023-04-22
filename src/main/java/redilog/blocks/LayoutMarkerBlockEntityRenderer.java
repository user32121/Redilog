package redilog.blocks;

import java.util.Collections;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import redilog.init.RedilogClient;

public class LayoutMarkerBlockEntityRenderer implements BlockEntityRenderer<LayoutMarkerBlockEntity> {

    //beam consts
    private static final float B_SIZE = 2;
    private static final float B_POS = (16 - B_SIZE) / 2;

    public static TexturedModelData createModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        root.addChild("main", ModelPartBuilder.create().cuboid(0, 0, 0, B_SIZE, B_SIZE, B_SIZE), ModelTransform.NONE);
        return TexturedModelData.of(modelData, (int) (B_SIZE * 4), (int) (B_SIZE * 2));
    }

    private final ModelPart model;

    public LayoutMarkerBlockEntityRenderer(Context context) {
        model = context.getLayerModelPart(RedilogClient.BEAM);
        model.pivotX = model.pivotY = model.pivotZ = B_POS;
    }

    @Override
    public void render(LayoutMarkerBlockEntity entity, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, int overlay) {
        //alignment indicators
        if (entity.getCachedState().get(LayoutMarkerBlock.POWERED)) {
            SpriteIdentifier spriteIdentifier = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
                    new Identifier("redilog", "block/layout_marker_beam1"));
            VertexConsumer vertexConsumer = spriteIdentifier.getVertexConsumer(vertexConsumers,
                    RenderLayer::getEntityCutout);
            //x
            model.xScale = 16 / B_SIZE * 128 + 1;
            model.pivotX = B_POS - 64 * 16;
            model.render(matrices, vertexConsumer, light, overlay);
            model.xScale = 1;
            model.pivotX = B_POS;
            //y
            model.yScale = 16 / B_SIZE * 128 + 1;
            model.pivotY = B_POS - 64 * 16;
            model.render(matrices, vertexConsumer, light, overlay);
            model.yScale = 1;
            model.pivotY = B_POS;
            //z
            model.zScale = 16 / B_SIZE * 128 + 1;
            model.pivotZ = B_POS - 64 * 16;
            model.render(matrices, vertexConsumer, light, overlay);
            model.zScale = 1;
            model.pivotZ = B_POS;
        }
        {
            //connections to this block
            SpriteIdentifier spriteIdentifier = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
                    new Identifier("redilog", "block/layout_marker_beam2"));
            VertexConsumer vertexConsumer = spriteIdentifier.getVertexConsumer(vertexConsumers,
                    RenderLayer::getEntityCutout);
            for (BlockPos connection : entity.connections) {
                BlockPos delta = connection.subtract(entity.getPos());
                int sharedAxes = (delta.getX() == 0 ? 1 : 0) + (delta.getY() == 0 ? 1 : 0)
                        + (delta.getZ() == 0 ? 1 : 0);
                //share 2 xyz values, and this is the lower 3rd value
                if (sharedAxes == 2 && entity.getPos().compareTo(connection) < 0) {
                    model.xScale = 16 / B_SIZE * delta.getX() + 1;
                    model.yScale = 16 / B_SIZE * delta.getY() + 1;
                    model.zScale = 16 / B_SIZE * delta.getZ() + 1;
                    model.render(matrices, vertexConsumer, light, overlay);
                    model.xScale = model.yScale = model.zScale = 1;
                }
            }
        }
        {
            //bounding box (only one block needs to render it, so choose "lowest")
            SpriteIdentifier spriteIdentifier = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
                    new Identifier("redilog", "block/layout_marker_beam3"));
            VertexConsumer vertexConsumer = spriteIdentifier.getVertexConsumer(vertexConsumers,
                    RenderLayer::getEntityCutout);
            if (!entity.connections.isEmpty() && entity.getPos().equals(Collections.min(entity.connections))) {
                Box box = new Box(entity.getPos());
                for (BlockPos connection : entity.connections) {
                    box = box.union(new Box(connection));
                }
                //set origin to bottom left of bounding box
                matrices.push();
                matrices.translate(box.minX - entity.getPos().getX(), box.minY - entity.getPos().getY(),
                        box.minZ - entity.getPos().getZ());
                //this corresponds to the 4 corners of each face, and we draw a beam along the third direction
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        model.xScale = 16 / B_SIZE * ((float) box.getXLength() - 1) + 1;
                        model.pivotY = B_POS + 16 * ((float) box.getYLength() - 1) * i;
                        model.pivotZ = B_POS + 16 * ((float) box.getZLength() - 1) * j;
                        model.render(matrices, vertexConsumer, light, overlay);
                        model.pivotY = B_POS;
                        model.xScale = 1;

                        model.pivotX = B_POS + 16 * ((float) box.getXLength() - 1) * i;
                        model.yScale = 16 / B_SIZE * ((float) box.getYLength() - 1) + 1;
                        model.render(matrices, vertexConsumer, light, overlay);
                        model.yScale = 1;
                        model.pivotZ = B_POS;

                        model.pivotY = B_POS + 16 * ((float) box.getYLength() - 1) * j;
                        model.zScale = 16 / B_SIZE * ((float) box.getZLength() - 1) + 1;
                        model.render(matrices, vertexConsumer, light, overlay);
                        model.zScale = 1;
                        model.pivotX = B_POS;
                        model.pivotY = B_POS;
                    }
                }
                matrices.pop();
            }
        }
    }

    @Override
    public boolean rendersOutsideBoundingBox(LayoutMarkerBlockEntity blockEntity) {
        return blockEntity.getCachedState().get(LayoutMarkerBlock.POWERED);
    }

    @Override
    public int getRenderDistance() {
        return 128;
    }
}
