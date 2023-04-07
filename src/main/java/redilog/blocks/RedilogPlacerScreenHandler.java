package redilog.blocks;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import redilog.init.Redilog;
import redilog.init.RedilogBlocks;

public class RedilogPlacerScreenHandler extends ScreenHandler {

    private final ScreenHandlerContext context;

    public RedilogPlacerScreenHandler(int syncId, ScreenHandlerContext context) {
        super(Redilog.REDILOG_PLACER_SCREEN_HANDLER, syncId);
        this.context = context;
    }

    public RedilogPlacerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, ScreenHandlerContext.EMPTY);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity var1, int var2) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return context.get(
                (world, pos) -> world.getBlockState(pos).isOf(RedilogBlocks.REDILOG_PLACER)
                        && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
                true);
    }
}
