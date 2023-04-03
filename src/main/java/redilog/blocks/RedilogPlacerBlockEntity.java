package redilog.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import redilog.init.RedilogBlocks;

public class RedilogPlacerBlockEntity extends BlockEntity {
    public RedilogPlacerBlockEntity(BlockPos pos, BlockState state) {
        super(RedilogBlocks.REDILOG_PLACER_ENTITY_TYPE, pos, state);
    }
}
