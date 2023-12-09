package redilog.synthesis.nodes;

import java.util.Collection;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.util.TriConsumer;

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

public class AndNode extends ComponentNode {
    public final Node input1, input2;
    protected boolean swapInputs;

    public AndNode(Expression owner, Node input1, Node input2) {
        super(owner);
        if (RedilogComponents.AND_GATE == null) {
            throw new IllegalStateException("Could not access and gate nbt");
        }
        this.input1 = input1;
        this.input2 = input2;
        input1.outputNodes.add(() -> VecUtil.i2d(getInput(0)));
        input2.outputNodes.add(() -> VecUtil.i2d(getInput(1)));
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid, Box buildSpace) {
        //swap inputs if more convenient
        swapInputs = input2.position.x < input1.position.x;

        super.placeAtPotentialPos(grid, buildSpace);
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes, Random rng) {
        //get average positions of inputs, outputs, and self
        Vec3d avgInputs = VecUtil.avg(input1.position, input2.position);
        Vec3d avgOutputs = VecUtil.avg(outputNodes.stream().map(s -> s.get()).toArray(Vec3d[]::new));
        position = VecUtil.avg(avgInputs, avgOutputs, avgOutputs, position);

        super.adjustPotentialPosition(buildSpace, otherNodes, rng);
    }

    @Override
    public void route(TriConsumer<Set<Vec4i>, Vec4i, Node> routeWire) throws RedilogPlacementException {
        if (input1 != null) {
            routeWire.accept(input1.getOutputs(), new Vec4i(getInput(0), 2), input1);
        }
        if (input2 != null) {
            routeWire.accept(input2.getOutputs(), new Vec4i(getInput(1), 2), input2);
        }
    }

    @Override
    public Component getComponent() {
        return RedilogComponents.AND_GATE;
    }

    @Override
    public Vec3i getInput(int index) {
        return super.getInput(swapInputs ? index ^ 0b1 : index);
    }
}
