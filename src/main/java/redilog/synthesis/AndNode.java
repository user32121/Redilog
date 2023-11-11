package redilog.synthesis;

import java.util.Collection;
import java.util.function.Supplier;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import redilog.parsing.Expression;
import redilog.routing.Placer.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class AndNode extends Node {
    private final static Array3D<BLOCK> AND_GATE_BLOCKS = new Array3D.Builder<BLOCK>()
            .size(3, 3, 3).data(new BLOCK[][][] {
                    { { BLOCK.BLOCK, BLOCK.BLOCK, BLOCK.BLOCK },
                            { BLOCK.REPEATER_SOUTH, BLOCK.BLOCK, BLOCK.BLOCK },
                            { BLOCK.AIR, BLOCK.TORCH, BLOCK.AIR }, },
                    { { BLOCK.AIR, BLOCK.AIR, BLOCK.BLOCK },
                            { BLOCK.AIR, BLOCK.BLOCK, BLOCK.TORCH_SOUTH },
                            { BLOCK.AIR, BLOCK.WIRE, BLOCK.AIR }, },
                    { { BLOCK.BLOCK, BLOCK.BLOCK, BLOCK.BLOCK },
                            { BLOCK.REPEATER_SOUTH, BLOCK.BLOCK, BLOCK.BLOCK },
                            { BLOCK.AIR, BLOCK.TORCH, BLOCK.AIR }, }, })
            .build();

    public final Node input1, input2;
    protected boolean swapInputs;

    public AndNode(Expression owner, Node input1, Node input2) {
        super(owner);
        this.input1 = input1;
        this.input2 = input2;
        input1.outputNodes.add(() -> VecUtil.i2d(getInput1()));
        input2.outputNodes.add(() -> VecUtil.i2d(getInput2()));
    }

    public Vec3i getInput1() {
        return VecUtil.d2i(position).add(swapInputs ? 2 : 0, 1, 0);
    }

    public Vec3i getInput2() {
        return VecUtil.d2i(position).add(swapInputs ? 0 : 2, 1, 0);
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid) {
        //swap inputs if more convenient
        swapInputs = input2.position.x < input1.position.x;

        outputs.add(new Vec4i(VecUtil.d2i(position).add(1, 1, 2), 14));

        for (BlockPos offset : BlockPos.iterate(BlockPos.ORIGIN,
                new BlockPos(AND_GATE_BLOCKS.getSize().add(-1, -1, -1)))) {
            grid.set(VecUtil.d2i(position).add(offset), AND_GATE_BLOCKS.get(offset));
        }
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes) {
        //get average positions of inputs and outputs
        Vec3d avg = position.add(input1.position).add(input2.position);
        int count = 3;
        for (Supplier<Vec3d> pos : outputNodes) {
            avg = avg.add(pos.get());
            ++count;
        }
        avg = avg.multiply(1.0 / count);

        //repel from other nodes
        for (Node n : otherNodes) {
            double distSqr = avg.squaredDistanceTo(n.position);
            avg = avg.lerp(n.position, -3 / (distSqr + 0.1));
        }

        //clamp by buildspace
        double x = avg.x;
        double y = avg.y;
        double z = avg.z;
        if (x < 0) {
            x = 0;
        } else if (x + AND_GATE_BLOCKS.getXLength() >= buildSpace.getXLength()) {
            x = buildSpace.getXLength() - AND_GATE_BLOCKS.getXLength();
        }
        if (y < 0) {
            y = 0;
        } else if (y + AND_GATE_BLOCKS.getYLength() >= buildSpace.getYLength()) {
            y = buildSpace.getYLength() - AND_GATE_BLOCKS.getYLength();
        }
        if (z < 2) {
            z = 2;
        } else if (z + AND_GATE_BLOCKS.getZLength() >= buildSpace.getZLength() - 3) {
            z = buildSpace.getZLength() - 3 - AND_GATE_BLOCKS.getZLength();
        }
        position = new Vec3d(x, y, z);
    }
}
