package redilog.blocks;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import redilog.init.Redilog;
import redilog.init.RedilogBlocks;

public class BuilderScreenHandler extends ScreenHandler {

    private final ScreenHandlerContext context;
    private final Identifier worldId;
    private final BlockPos pos;
    private final String originalText;

    //called on server
    public BuilderScreenHandler(int syncId, ScreenHandlerContext context) {
        super(Redilog.BUILDER_SCREEN_HANDLER, syncId);
        this.context = context;
        this.worldId = World.OVERWORLD.getValue();
        this.pos = BlockPos.ORIGIN;
        this.originalText = "";
    }

    //called on client
    public BuilderScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        super(Redilog.BUILDER_SCREEN_HANDLER, syncId);
        this.context = ScreenHandlerContext.EMPTY;
        this.worldId = buf.readIdentifier();
        this.pos = buf.readBlockPos();
        this.originalText = buf.readString();
    }

    public Identifier getWorldId() {
        return worldId;
    }

    @Override
    public ItemStack transferSlot(PlayerEntity var1, int var2) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return context.get(
                (world, pos) -> world.getBlockState(pos).isOf(RedilogBlocks.BUILDER)
                        && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
                true);
    }

    public ScreenHandlerContext getContext() {
        return context;
    }

    public BlockPos getPos() {
        return pos;
    }

    public String getOriginalText() {
        return originalText;
    }
}
