package redilog.synthesis;

import java.util.Collection;
import java.util.function.Supplier;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import redilog.parsing.BitwiseOrExpression;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class OrNode extends Node {
    private final static Array3D<BLOCK> orGateBlocks = new Array3D.Builder<BLOCK>().size(3, 2, 3).data(new BLOCK[][][] {
            { { BLOCK.BLOCK, BLOCK.BLOCK, BLOCK.BLOCK },
                    { BLOCK.WIRE, BLOCK.REPEATER_SOUTH, BLOCK.WIRE }, },
            { { BLOCK.AIR, BLOCK.AIR, BLOCK.BLOCK },
                    { BLOCK.AIR, BLOCK.AIR, BLOCK.WIRE }, },
            { { BLOCK.BLOCK, BLOCK.BLOCK, BLOCK.BLOCK },
                    { BLOCK.WIRE, BLOCK.REPEATER_SOUTH, BLOCK.WIRE }, }, })
            .build();

    public final Node input1, input2;
    private boolean swapInputs;

    public OrNode(BitwiseOrExpression owner, Node input1, Node input2) {
        super(owner);
        this.input1 = input1;
        this.input2 = input2;
        input1.outputNodes.add(() -> VecUtil.i2d(getInput1()));
        input2.outputNodes.add(() -> VecUtil.i2d(getInput2()));
    }

    public Vec3i getInput1() {
        return VecUtil.d2i(getPosition()).add(swapInputs ? 2 : 0, 1, 0);
    }

    public Vec3i getInput2() {
        return VecUtil.d2i(getPosition()).add(swapInputs ? 0 : 2, 1, 0);
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid) {
        //swap inputs if more convenient
        swapInputs = input2.getPosition().x < input1.getPosition().x;

        outputs.add(new Vec4i(VecUtil.d2i(getPosition()).add(0, 1, 2), 13));
        outputs.add(new Vec4i(VecUtil.d2i(getPosition()).add(1, 1, 2), 14));
        outputs.add(new Vec4i(VecUtil.d2i(getPosition()).add(2, 1, 2), 13));

        for (BlockPos offset : BlockPos.iterate(BlockPos.ORIGIN,
                new BlockPos(orGateBlocks.getSize().add(-1, -1, -1)))) {
            grid.set(VecUtil.d2i(getPosition()).add(offset), orGateBlocks.get(offset));
        }
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes) {
        //get average positions of inputs and outputs
        Vec3d avg = getPosition().add(input1.getPosition()).add(input2.getPosition());
        int count = 3;
        for (Supplier<Vec3d> pos : outputNodes) {
            avg = avg.add(pos.get());
            ++count;
        }
        avg = avg.multiply(1.0 / count);

        //repel from other nodes
        for (Node n : otherNodes) {
            double distSqr = avg.squaredDistanceTo(n.getPosition());
            avg = avg.lerp(n.getPosition(), -3 / (distSqr + 0.1));
        }

        //clamp by buildspace
        double x = avg.x;
        double y = avg.y;
        double z = avg.z;
        if (x < 0) {
            x = 0;
        } else if (x + orGateBlocks.getXLength() >= buildSpace.getXLength()) {
            x = buildSpace.getXLength() - orGateBlocks.getXLength();
        }
        if (y < 0) {
            y = 0;
        } else if (y + orGateBlocks.getYLength() >= buildSpace.getYLength()) {
            y = buildSpace.getYLength() - orGateBlocks.getYLength();
        }
        if (z < 2) {
            z = 2;
        } else if (z + orGateBlocks.getZLength() >= buildSpace.getZLength() - 3) {
            z = buildSpace.getZLength() - 3 - orGateBlocks.getZLength();
        }
        setPosition(new Vec3d(x, y, z));
    }
}
