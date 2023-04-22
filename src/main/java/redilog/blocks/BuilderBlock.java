package redilog.blocks;

import java.util.Set;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import redilog.init.Redilog;

public class BuilderBlock extends BlockWithEntity {

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

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        if (world.isClient) {
            return;
        }
        if (!(world.getBlockEntity(pos) instanceof BuilderBlockEntity bbe)) {
            return;
        }
        LayoutMarkerBlockEntity lmbe = null;
        for (Direction dir : (Iterable<Direction>) Direction.stream()::iterator) {
            if (world.getBlockEntity(pos.offset(dir)) instanceof LayoutMarkerBlockEntity lmbe2) {
                lmbe = lmbe2;
                break;
            }
        }
        if (lmbe != null) {
            bbe.setBuildSpace(lmbe.getLayoutBox());
            //make a copy since modifying while iterating is undefined (or an exception)
            Set<BlockPos> connections = Set.copyOf(lmbe.connections);
            for (BlockPos marker : connections) {
                world.breakBlock(marker, false);
            }
        }
    }
}
