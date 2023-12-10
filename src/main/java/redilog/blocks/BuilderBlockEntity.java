package redilog.blocks;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import redilog.init.RedilogBlocks;
import redilog.utils.LoggerUtil;

public class BuilderBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {

    private static final String REDILOG_KEY = "Redilog";
    private static final String BUILD_MIN_KEY = "BuildSpaceMin";
    private static final String BUILD_MAX_KEY = "BuildSpaceMax";

    private String redilog = "";
    private Box buildSpace = new Box(0, 0, 0, 0, 0, 0);
    BuilderRunnable currentBuild = null;
    Thread currentBuildThread = null;

    public BuilderBlockEntity(BlockPos pos, BlockState state) {
        super(RedilogBlocks.BUILDER_ENTITY, pos, state);
    }

    public void setBuildSpace(Box value) {
        this.buildSpace = value;
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

    public void build(ServerPlayerEntity player) {
        if (currentBuildThread != null && currentBuildThread.isAlive()) {
            //cancel previous build
            LoggerUtil.logWarnAndCreateMessage(player::sendMessage, "Cancelling last build.");
            currentBuild.shouldStop = true;
        }
        currentBuild = new BuilderRunnable(this, player, redilog, buildSpace, world);
        currentBuildThread = new Thread(currentBuild);
        currentBuildThread.start();
        world.createAndScheduleBlockTick(pos, RedilogBlocks.BUILDER, 1);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        redilog = nbt.getString(REDILOG_KEY);
        buildSpace = new Box(NbtHelper.toBlockPos(nbt.getCompound(BUILD_MIN_KEY)),
                NbtHelper.toBlockPos(nbt.getCompound(BUILD_MAX_KEY)));
    }

    public Box getBuildSpace() {
        return this.buildSpace;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    public void scheduledTick() {
        if (currentBuildThread == null) {
            return;
        }
        //wait until thread is done
        if (currentBuildThread.isAlive()) {
            world.createAndScheduleBlockTick(pos, RedilogBlocks.BUILDER, 1);
            return;
        }
        //perform world operations on main thread
        if (!currentBuild.mainThreadOperations()) {
            world.createAndScheduleBlockTick(pos, RedilogBlocks.BUILDER, 1);
            return;
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString(REDILOG_KEY, redilog);
        nbt.put(BUILD_MIN_KEY, NbtHelper.fromBlockPos(new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ)));
        nbt.put(BUILD_MAX_KEY, NbtHelper.fromBlockPos(new BlockPos(buildSpace.maxX, buildSpace.maxY, buildSpace.maxZ)));
    }
}
