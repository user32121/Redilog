package redilog.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LayoutMarkerBlock extends BlockWithEntity {
    public static final BooleanProperty POWERED = Properties.POWERED;

    public LayoutMarkerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(POWERED, false));
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos,
            boolean notify) {
        if (world.isClient) {
            return;
        }
        boolean bl = state.get(POWERED);
        if (bl != world.isReceivingRedstonePower(pos)) {
            world.setBlockState(pos, state.cycle(POWERED), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LayoutMarkerBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof LayoutMarkerBlockEntity lmbe) {
            lmbe.recalculateConnections();
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void appendProperties(Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POWERED);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof LayoutMarkerBlockEntity lmbe) {
            lmbe.connections.remove(pos);
            lmbe.markRemoved();
            for (BlockPos connection : lmbe.connections) {
                world.updateListeners(connection, state, newState, Block.NOTIFY_LISTENERS);
            }
        }
    }
}
