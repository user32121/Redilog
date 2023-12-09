package redilog.synthesis.nodes;

import java.util.Collection;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import redilog.init.RedilogComponents;
import redilog.parsing.expressions.Expression;
import redilog.routing.BLOCK;
import redilog.routing.RedilogPlacementException;
import redilog.utils.Array3D;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class OrNode extends Node {
    public final Node input1, input2;
    protected boolean swapInputs;

    public OrNode(Expression owner, Node input1, Node input2) {
        super(owner);
        if (RedilogComponents.NOT_GATE == null) {
            throw new IllegalStateException("Could not access or gate nbt");
        }
        this.input1 = input1;
        this.input2 = input2;
        input1.outputNodes.add(() -> VecUtil.i2d(getInput(0)));
        input2.outputNodes.add(() -> VecUtil.i2d(getInput(1)));
    }

    public Vec3i getInput(int index) {
        return VecUtil.d2i(position).add(RedilogComponents.OR_GATE.inputs.get(index));
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid) {
        //swap inputs if more convenient
        swapInputs = input2.position.x < input1.position.x;

        for (Vec4i output : RedilogComponents.OR_GATE.outputs) {
            outputs.add(new Vec4i(VecUtil.d2i(position), 0).add(output));
        }

        for (BlockPos offset : BlockPos.iterate(BlockPos.ORIGIN,
                new BlockPos(RedilogComponents.OR_GATE.blocks.getSize().add(-1, -1, -1)))) {
            BLOCK b = RedilogComponents.OR_GATE.blocks.get(offset);
            if (b != null) {
                grid.set(VecUtil.d2i(position).add(offset), b);
            }
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
        } else if (x + RedilogComponents.OR_GATE.blocks.getXLength() >= buildSpace.getXLength()) {
            x = buildSpace.getXLength() - RedilogComponents.OR_GATE.blocks.getXLength();
        }
        if (y < 0) {
            y = 0;
        } else if (y + RedilogComponents.OR_GATE.blocks.getYLength() >= buildSpace.getYLength()) {
            y = buildSpace.getYLength() - RedilogComponents.OR_GATE.blocks.getYLength();
        }
        if (z < 2) {
            z = 2;
        } else if (z + RedilogComponents.OR_GATE.blocks.getZLength() >= buildSpace.getZLength() - 3) {
            z = buildSpace.getZLength() - 3 - RedilogComponents.OR_GATE.blocks.getZLength();
        }
        position = new Vec3d(x, y, z);
    }

    @Override
    public void route(TriConsumer<Set<Vec4i>, Vec4i, Node> routeWire) throws RedilogPlacementException {
        if (input1 != null) {
            routeWire.accept(input1.getOutputs(), new Vec4i(getInput(0), 1), input1);
        }
        if (input2 != null) {
            routeWire.accept(input2.getOutputs(), new Vec4i(getInput(1), 1), input2);
        }
    }
}
