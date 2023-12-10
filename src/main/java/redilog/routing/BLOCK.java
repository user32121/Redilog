package redilog.routing;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.util.math.Direction;

public enum BLOCK {
    AIR(Blocks.AIR.getDefaultState()),
    STRICT_AIR(Blocks.AIR.getDefaultState(), Blocks.WHITE_STAINED_GLASS.getDefaultState()), //a block that must be air, such as above diagonal wires
    WIRE(Blocks.REDSTONE_WIRE.getStateManager().getStates().toArray(BlockState[]::new)),
    BLOCK(Blocks.LIGHT_BLUE_CONCRETE.getDefaultState()),
    REPEATER_NORTH(Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.SOUTH)),
    REPEATER_SOUTH(Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.NORTH)),
    REPEATER_EAST(Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.WEST)),
    REPEATER_WEST(Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.EAST)),
    REDSTONE_BLOCK(Blocks.REDSTONE_BLOCK.getDefaultState()),
    TORCH(Blocks.REDSTONE_TORCH.getDefaultState(),
            Blocks.REDSTONE_TORCH.getDefaultState().with(RedstoneTorchBlock.LIT, false)),
    TORCH_NORTH(Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, Direction.NORTH),
            Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, Direction.NORTH)
                    .with(WallRedstoneTorchBlock.LIT, false)),
    TORCH_SOUTH(Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, Direction.SOUTH),
            Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, Direction.SOUTH)
                    .with(WallRedstoneTorchBlock.LIT, false)),
    TORCH_EAST(Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, Direction.EAST),
            Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, Direction.EAST)
                    .with(WallRedstoneTorchBlock.LIT, false)),
    TORCH_WEST(Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, Direction.WEST),
            Blocks.REDSTONE_WALL_TORCH.getDefaultState().with(WallRedstoneTorchBlock.FACING, Direction.WEST)
                    .with(WallRedstoneTorchBlock.LIT, false));

    public BlockState[] states;

    private BLOCK(BlockState... states) {
        if (states.length < 1) {
            throw new AssertionError(String.format("%s must have at least 1 state", this));
        }
        this.states = states;
    }

    public boolean matches(BlockState state) {
        for (BlockState s : states) {
            if (state == s) {
                return true;
            }
        }
        return false;
    }

    public static BLOCK fromState(BlockState state) {
        if (state.getBlock() == Blocks.STRUCTURE_VOID) {
            //special case
            return null;
        }
        for (BLOCK b : values()) {
            if (b.matches(state)) {
                return b;
            }
        }
        throw new AssertionError(String.format("Could not match %s to any BLOCKs in enum", state));
    }
}