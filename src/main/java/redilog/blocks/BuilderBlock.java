package redilog.blocks;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import redilog.init.Redilog;

public class BuilderBlock extends BlockWithEntity {
    public BuilderBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BuilderBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        if (!world.isClient) {
            player.openHandledScreen(state.createScreenHandlerFactory(world, pos));
        }
        return ActionResult.SUCCESS;
    }

    public static void RegisterRedilogSyncPacketReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(Redilog.BUILDER_SYNC_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    Identifier worldId = buf.readIdentifier();
                    BlockPos pos = buf.readBlockPos();
                    String redilog = buf.readString();
                    boolean shouldBuild = buf.readBoolean();
                    server.execute(() -> {
                        ServerWorld world = server.getWorld(RegistryKey.of(Registry.WORLD_KEY, worldId));
                        if (world.getBlockEntity(pos) instanceof BuilderBlockEntity bbe) {
                            bbe.setRedilog(redilog);
                            if (shouldBuild) {
                                bbe.build();
                            }
                        }
                    });
                });
    }
}
