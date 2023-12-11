package redilog.synthesis.nodes;

import java.util.Set;
import java.util.function.Consumer;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraft.block.Blocks;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
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

public class OutputNode extends IONode {
    public final Node input;

    public OutputNode(Expression owner, String name, Node value) {
        super(owner, name);
        this.input = value;
        if (value != null) {
            value.outputNodes.add(this::getPosition);
        }
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid, Box buildSpace) {
        grid.set(VecUtil.d2i(getPosition()), BLOCK.WIRE);
        grid.set(VecUtil.d2i(getPosition().add(0, -1, 0)), BLOCK.BLOCK);
    }

    @Override
    public Set<Vec4i> getOutputs() throws RedilogPlacementException {
        throw new RedilogPlacementException("Cannot use output node as intermediate node");
    }

    @Override
    public void route(TriConsumer<Set<Vec4i>, Vec4i, Node> routeWire) throws RedilogPlacementException {
        if (input != null) {
            routeWire.accept(input.getOutputs(), new Vec4i(VecUtil.d2i(position), 1), input);
        }
    }

    @Override
    public void placeLabel(World world, BlockPos relativeOrigin, Consumer<Text> feedback) {
        Vec3i pos = VecUtil.d2i(position);
        if (pos == null) {
            LoggerUtil.logWarnAndCreateMessage(feedback, String.format("Failed to label output %s", name));
            return;
        }
        world.setBlockState(relativeOrigin.add(pos.down()), Blocks.REDSTONE_LAMP.getDefaultState());
        world.setBlockState(relativeOrigin.add(pos), Blocks.REDSTONE_WIRE.getDefaultState());
        world.setBlockState(relativeOrigin.add(pos).add(0, -1, 1),
                Blocks.BIRCH_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, Direction.SOUTH));
        if (world.getBlockEntity(relativeOrigin.add(pos).add(0, -1, 1)) instanceof SignBlockEntity sbe) {
            sbe.setTextOnRow(0, Text.of(name));
        }
    }

    @Override
    public Box getBoundingBox() {
        return new Box(new BlockPos(position));
    }
}