package redilog.blocks;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import redilog.init.RedilogBlocks;

public class BuilderBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {
    private static final String REDILOG_KEY = "Redilog";

    private String redilog = "";

    public BuilderBlockEntity(BlockPos pos, BlockState state) {
        super(RedilogBlocks.BUILDER_ENTITY, pos, state);
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BuilderScreenHandler(syncId, ScreenHandlerContext.create(world, pos));
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeIdentifier(player.getWorld().getDimensionKey().getValue());
        buf.writeBlockPos(pos);
        buf.writeString(redilog);
    }

    public void setRedilog(String value) {
        this.redilog = value;
        markDirty();
    }

    public void build() {
        world.setBlockState(pos.add(1, 0, 0), Blocks.REDSTONE_WIRE.getDefaultState());
        world.setBlockState(pos.add(1, 0, 1), Blocks.REDSTONE_WIRE.getDefaultState());
        world.setBlockState(pos.add(0, 0, 1), Blocks.REDSTONE_WIRE.getDefaultState());
        world.setBlockState(pos.add(-1, 0, 1), Blocks.REDSTONE_WIRE.getDefaultState());
        world.setBlockState(pos.add(-1, 0, 0), Blocks.REDSTONE_WIRE.getDefaultState());
        world.setBlockState(pos.add(-1, 0, -1), Blocks.REDSTONE_WIRE.getDefaultState());
        world.setBlockState(pos.add(0, 0, -1), Blocks.REDSTONE_WIRE.getDefaultState());
        world.setBlockState(pos.add(1, 0, -1), Blocks.REDSTONE_WIRE.getDefaultState());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        redilog = nbt.getString(REDILOG_KEY);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString(REDILOG_KEY, redilog);
    }
}
