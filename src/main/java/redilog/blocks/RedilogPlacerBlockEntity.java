package redilog.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import redilog.init.RedilogBlocks;

public class RedilogPlacerBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    public RedilogPlacerBlockEntity(BlockPos pos, BlockState state) {
        super(RedilogBlocks.REDILOG_PLACER_ENTITY_TYPE, pos, state);
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new RedilogPlacerScreenHandler(syncId, playerInventory);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }
}
