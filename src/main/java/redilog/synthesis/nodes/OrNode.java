package redilog.synthesis.nodes;

import java.util.Collection;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import redilog.parsing.expressions.Expression;
import redilog.routing.BLOCK;
import redilog.routing.RedilogPlacementException;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class OrNode extends Node {
    //TODO nbt
    private final static Array3D<BLOCK> OR_GATE_BLOCKS = new Array3D.Builder<BLOCK>()
            .data(new BLOCK[][][] {
                    { { BLOCK.BLOCK, BLOCK.BLOCK, BLOCK.BLOCK },
                            { BLOCK.WIRE, BLOCK.REPEATER_SOUTH, BLOCK.WIRE }, },
                    { { BLOCK.AIR, BLOCK.AIR, BLOCK.BLOCK },
                            { BLOCK.AIR, BLOCK.AIR, BLOCK.WIRE }, },
                    { { BLOCK.BLOCK, BLOCK.BLOCK, BLOCK.BLOCK },
                            { BLOCK.WIRE, BLOCK.REPEATER_SOUTH, BLOCK.WIRE }, }, })
            .build();

    public final Node input1, input2;
    protected boolean swapInputs;

    public OrNode(Expression owner, Node input1, Node input2) {
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

        outputs.add(new Vec4i(VecUtil.d2i(position).add(0, 1, 2), 13));
        outputs.add(new Vec4i(VecUtil.d2i(position).add(1, 1, 2), 14));
        outputs.add(new Vec4i(VecUtil.d2i(position).add(2, 1, 2), 13));

        for (BlockPos offset : BlockPos.iterate(BlockPos.ORIGIN,
                new BlockPos(OR_GATE_BLOCKS.getSize().add(-1, -1, -1)))) {
            grid.set(VecUtil.d2i(position).add(offset), OR_GATE_BLOCKS.get(offset));
        }
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes, Random rng) {
        //get average positions of inputs, outputs, and self
        Vec3d avgInputs = VecUtil.avg(input1.position, input2.position);
        Vec3d avgOutputs = VecUtil.avg(outputNodes.stream().map(s -> s.get()).toArray(Vec3d[]::new));
        Vec3d avg = VecUtil.avg(avgInputs, avgOutputs, avgOutputs, position);
        avg = avg.add(rng.nextDouble(-1, 1), rng.nextDouble(-1, 1), rng.nextDouble(-1, 1));

        //repel from other nodes
        for (Node n : otherNodes) {
            double distSqr = avg.squaredDistanceTo(n.position);
            avg = avg.lerp(n.position, -5 / (distSqr + 1));
        }

        //clamp by buildspace
        double x = avg.x;
        double y = avg.y;
        double z = avg.z;
        if (x < 0) {
            x = 0;
        } else if (x + OR_GATE_BLOCKS.getXLength() >= buildSpace.getXLength()) {
            x = buildSpace.getXLength() - OR_GATE_BLOCKS.getXLength();
        }
        if (y < 0) {
            y = 0;
        } else if (y + OR_GATE_BLOCKS.getYLength() >= buildSpace.getYLength()) {
            y = buildSpace.getYLength() - OR_GATE_BLOCKS.getYLength();
        }
        if (z < 2) {
            z = 2;
        } else if (z + OR_GATE_BLOCKS.getZLength() >= buildSpace.getZLength() - 3) {
            z = buildSpace.getZLength() - 3 - OR_GATE_BLOCKS.getZLength();
        }
        position = new Vec3d(x, y, z);
    }

    @Override
    public void route(TriConsumer<Set<Vec4i>, Vec4i, Node> routeWire) throws RedilogPlacementException {
        if (input1 != null) {
            routeWire.accept(input1.getOutputs(), new Vec4i(getInput1(), 1), input1);
        }
        if (input2 != null) {
            routeWire.accept(input2.getOutputs(), new Vec4i(getInput2(), 1), input2);
        }
    }
}
