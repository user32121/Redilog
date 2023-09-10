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
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import redilog.init.Redilog;
import redilog.init.RedilogBlocks;
import redilog.parsing.Parser;
import redilog.parsing.RedilogParsingException;
import redilog.parsing.SymbolGraph;
import redilog.routing.Placer;
import redilog.routing.RedilogPlacementException;
import redilog.synthesis.LogicGraph;
import redilog.synthesis.RedilogSynthesisException;
import redilog.synthesis.Synthesizer;

public class BuilderBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {
    private static final String REDILOG_KEY = "Redilog";
    private static final String BUILD_MIN_KEY = "BuildSpaceMin";
    private static final String BUILD_MAX_KEY = "BuildSpaceMax";

    private String redilog = "";
    private Box buildSpace = new Box(0, 0, 0, 0, 0, 0);

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
        try {
            Redilog.LOGGER.info("Begin parsing stage");
            player.sendMessage(Text.of("parsing..."));
            SymbolGraph sGraph = Parser.parseRedilog(redilog, player::sendMessage);
            Redilog.LOGGER.info("Begin synthesize stage");
            player.sendMessage(Text.of("synthesizing..."));
            LogicGraph lGraph = Synthesizer.synthesize(sGraph, player::sendMessage);
            Redilog.LOGGER.info("Begin placing and routing stage");
            player.sendMessage(Text.of("placing..."));
            Placer.placeRedilog(lGraph, buildSpace, world, player::sendMessage);

            player.sendMessage(Text.of("Build finished."));
        } catch (RedilogParsingException e) {
            player.sendMessage(Text.literal("An error occurred during parsing.\n")
                    .append(Text.literal(e.toString()).setStyle(Style.EMPTY.withColor(Formatting.RED))));
            Redilog.LOGGER.error("An error occurred during parsing", e);
            return;
        } catch (RedilogSynthesisException e) {
            player.sendMessage(Text.literal("An error occurred during synthesis.\n")
                    .append(Text.literal(e.toString()).setStyle(Style.EMPTY.withColor(Formatting.RED))));
            Redilog.LOGGER.error("An error occurred during synthesis", e);
            return;
        } catch (RedilogPlacementException e) {
            player.sendMessage(Text.literal("An error occurred during placement.\n")
                    .append(Text.literal(e.toString()).setStyle(Style.EMPTY.withColor(Formatting.RED))));
            Redilog.LOGGER.error("An error occurred during placement", e);
            return;
        } catch (Exception e) {
            player.sendMessage(Text.of("An internal error occurred. See server log for more details."));
            Redilog.LOGGER.error("An internal error occurred", e);
            return;
        }
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

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString(REDILOG_KEY, redilog);
        nbt.put(BUILD_MIN_KEY, NbtHelper.fromBlockPos(new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ)));
        nbt.put(BUILD_MAX_KEY, NbtHelper.fromBlockPos(new BlockPos(buildSpace.maxX, buildSpace.maxY, buildSpace.maxZ)));
    }
}
