package redilog.synthesis.nodes;

import java.util.Set;
import java.util.function.Consumer;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import redilog.parsing.expressions.Expression;
import redilog.routing.BLOCK;
import redilog.routing.RedilogPlacementException;
import redilog.utils.Array3D;
import redilog.utils.LoggerUtil;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class InputNode extends IONode {

    public InputNode(Expression owner, String name) {
        super(owner, name);
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid, Box buildSpace) {
        grid.set(VecUtil.d2i(getPosition()).add(0, -1, 0), BLOCK.BLOCK);
        grid.set(VecUtil.d2i(getPosition()).add(0, -1, 1), BLOCK.BLOCK);
        grid.set(VecUtil.d2i(getPosition()).add(0, 0, 1), BLOCK.WIRE);
        outputs.add(new Vec4i(VecUtil.d2i(getPosition()).add(0, 0, 1), 15));
    }

    @Override
    public void route(TriConsumer<Set<Vec4i>, Vec4i, Node> routeWire) throws RedilogPlacementException {
        // NO OP
    }

    @Override
    public void placeLabel(World world, BlockPos relativeOrigin, Consumer<Text> feedback) {
        Vec3i pos = VecUtil.d2i(position);
        if (pos == null) {
            LoggerUtil.logWarnAndCreateMessage(feedback, String.format("Failed to label input %s", name));
            return;
        }
        world.setBlockState(relativeOrigin.add(pos.down()), Blocks.WHITE_CONCRETE.getDefaultState());
        world.setBlockState(relativeOrigin.add(pos),
                Blocks.LEVER.getDefaultState().with(LeverBlock.FACE, WallMountLocation.FLOOR));
        world.setBlockState(relativeOrigin.add(pos).add(0, -1, -1),
                Blocks.BIRCH_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, Direction.NORTH));
        if (world.getBlockEntity(relativeOrigin.add(pos).add(0, -1, -1)) instanceof SignBlockEntity sbe) {
            sbe.setTextOnRow(0, Text.of(name));
        }
    }

    @Override
    public Box getBoundingBox() {
        return new Box(position, position.add(1, 2, 2));
    }
}