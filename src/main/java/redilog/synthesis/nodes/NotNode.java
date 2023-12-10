package redilog.synthesis.nodes;

import java.util.Collection;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import redilog.init.RedilogComponents;
import redilog.parsing.expressions.Expression;
import redilog.routing.RedilogPlacementException;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class NotNode extends ComponentNode {
    public final Node input1;

    public NotNode(Expression owner, Node input1) {
        super(owner);
        if (RedilogComponents.NOT_GATE == null) {
            throw new IllegalStateException("Could not access not gate nbt");
        }
        this.input1 = input1;
        input1.outputNodes.add(() -> VecUtil.i2d(getInput(0)));
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes, Random rng) {
        //get average positions of inputs, outputs, and self
        Vec3d avgInputs = VecUtil.avg(input1.position);
        Vec3d avgOutputs = VecUtil.avg(outputNodes.stream().map(s -> s.get()).toArray(Vec3d[]::new));
        position = VecUtil.avg(avgInputs, avgOutputs, position);

        super.adjustPotentialPosition(buildSpace, otherNodes, rng);
    }

    @Override
    public void route(TriConsumer<Set<Vec4i>, Vec4i, Node> routeWire) throws RedilogPlacementException {
        if (input1 != null) {
            routeWire.accept(input1.getOutputs(), new Vec4i(getInput(0), 2), input1);
        }
    }

    @Override
    public Component getComponent() {
        return RedilogComponents.NOT_GATE;
    }
}
