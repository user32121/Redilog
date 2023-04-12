package redilog.blocks;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
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
import redilog.init.Redilog;
import redilog.init.RedilogBlocks;
import redilog.routing.Placer;
import redilog.routing.RedilogPlacementException;
import redilog.synthesis.Graph;
import redilog.synthesis.Parser;
import redilog.synthesis.RedilogParsingException;

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
        Graph graph;
        try {
            graph = Parser.parseRedilog(redilog);
            Placer.placeRedilog(graph, world);
        } catch (RedilogParsingException e) {
            // TODO notify user
            Redilog.LOGGER.error("An error occurred during parsing", e);
            return;
        } catch (RedilogPlacementException e) {
            //TODO notify user
            Redilog.LOGGER.error("An error occurred during placement", e);
            return;
        } catch (Exception e) {
            //TODO notify user
            Redilog.LOGGER.error("An internal error occurred", e);
            return;
        }
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
