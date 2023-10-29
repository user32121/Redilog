package redilog.synthesis;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import redilog.parsing.BitwiseOrExpression;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;

public class OrNode extends Node {
    public Node input1, input2;
    public Vec3i position;
    public Set<Vec4i> outputs = new HashSet<>();

    public OrNode(BitwiseOrExpression owner, Node input1, Node input2) {
        super(owner);
        this.input1 = input1;
        this.input2 = input2;
    }

    public boolean isPlaced() {
        return position != null;
    }

    public Vec3i getInput1() {
        return position.add(-1, 0, 0);
    }

    public Vec3i getInput2() {
        return position.add(1, 0, 0);
    }

    @Override
    public void placeAt(Array3D<BLOCK> grid, Vec3i pos) {
        position = pos.add(1, 1, 0);
        outputs.add(new Vec4i(pos.add(0, 1, 2), 13));
        outputs.add(new Vec4i(pos.add(1, 1, 2), 14));
        outputs.add(new Vec4i(pos.add(2, 1, 2), 13));
        BLOCK[][][] orGateBlocks = {
                { { BLOCK.BLOCK, BLOCK.BLOCK, BLOCK.BLOCK },
                        { BLOCK.WIRE, BLOCK.REPEATER_SOUTH, BLOCK.WIRE }, },
                { { BLOCK.AIR, BLOCK.AIR, BLOCK.BLOCK },
                        { BLOCK.AIR, BLOCK.AIR, BLOCK.WIRE }, },
                { { BLOCK.BLOCK, BLOCK.BLOCK, BLOCK.BLOCK },
                        { BLOCK.WIRE, BLOCK.REPEATER_SOUTH, BLOCK.WIRE }, }, };
        for (BlockPos offset : BlockPos.iterate(0, 0, 0,
                orGateBlocks.length - 1, orGateBlocks[0].length - 1, orGateBlocks[0][0].length - 1)) {
            grid.set(pos.add(offset), orGateBlocks[offset.getX()][offset.getY()][offset.getZ()]);
        }
    }

    @Override
    public Set<Vec4i> getOutputs() {
        return outputs;
    }

}
