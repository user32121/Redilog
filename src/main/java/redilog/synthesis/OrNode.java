package redilog.synthesis;

import java.util.Collection;

import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import redilog.parsing.BitwiseOrExpression;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class OrNode extends Node {
    public final Node input1, input2;
    private Vec3i position;

    public OrNode(BitwiseOrExpression owner, Node input1, Node input2) {
        super(owner);
        this.input1 = input1;
        this.input2 = input2;
        input1.outputNodes.add(this);
        input2.outputNodes.add(this);
    }

    public boolean isPlaced() {
        return position != null;
    }

    public Vec3i getInput1() {
        return position;
    }

    public Vec3i getInput2() {
        return position.add(2, 0, 0);
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid) {
        Vec3i position = VecUtil.vec3f2i(potentialPosition);
        outputs.add(new Vec4i(position.add(0, 1, 2), 13));
        outputs.add(new Vec4i(position.add(1, 1, 2), 14));
        outputs.add(new Vec4i(position.add(2, 1, 2), 13));
        BLOCK[][][] orGateBlocks = {
                { { BLOCK.BLOCK, BLOCK.BLOCK, BLOCK.BLOCK },
                        { BLOCK.WIRE, BLOCK.REPEATER_SOUTH, BLOCK.WIRE }, },
                { { BLOCK.AIR, BLOCK.AIR, BLOCK.BLOCK },
                        { BLOCK.AIR, BLOCK.AIR, BLOCK.WIRE }, },
                { { BLOCK.BLOCK, BLOCK.BLOCK, BLOCK.BLOCK },
                        { BLOCK.WIRE, BLOCK.REPEATER_SOUTH, BLOCK.WIRE }, }, };
        for (BlockPos offset : BlockPos.iterate(0, 0, 0,
                orGateBlocks.length - 1, orGateBlocks[0].length - 1, orGateBlocks[0][0].length - 1)) {
            grid.set(position.add(offset), orGateBlocks[offset.getX()][offset.getY()][offset.getZ()]);
        }
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("Unimplemented method 'adjustPotentialPosition'");
    }

}
