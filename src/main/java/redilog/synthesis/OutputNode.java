package redilog.synthesis;

import java.util.Set;
import java.util.function.Consumer;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraft.block.Blocks;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import redilog.parsing.Expression;
import redilog.routing.Placer.BLOCK;
import redilog.routing.RedilogPlacementException;
import redilog.utils.Array3D;
import redilog.utils.LoggerUtil;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class OutputNode extends IONode {
    public final Node value;

    public OutputNode(Expression owner, String name, Node value) {
        super(owner, name);
        this.used = true;
        this.value = value;
        if (value != null) {
            value.outputNodes.add(() -> getPosition());
        }
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid) {
        grid.set(VecUtil.d2i(getPosition()), BLOCK.WIRE);
        grid.set(VecUtil.d2i(getPosition().add(0, -1, 0)), BLOCK.BLOCK);
    }

    @Override
    public Set<Vec4i> getOutputs() throws RedilogPlacementException {
        throw new RedilogPlacementException("Cannot use output node as intermediate node");
    }

    @Override
    public void routeBFS(TriConsumer<Set<Vec4i>, Vec4i, Node> bfs) throws RedilogPlacementException {
        if (value != null) {
            bfs.accept(value.getOutputs(), new Vec4i(VecUtil.d2i(position), 1), value);
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
}