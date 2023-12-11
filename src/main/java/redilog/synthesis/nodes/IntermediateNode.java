package redilog.synthesis.nodes;

import java.util.Collection;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import redilog.parsing.expressions.Expression;
import redilog.routing.BLOCK;
import redilog.routing.RedilogPlacementException;
import redilog.utils.Array3D;
import redilog.utils.MathUtil;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class IntermediateNode extends Node {
    public final Node input;

    public IntermediateNode(Expression owner, Node value) {
        super(owner);
        this.input = value;
        if (value != null) {
            value.outputNodes.add(this::getPosition);
        }
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid, Box buildSpace) {
        clampToBuildSpace(buildSpace);

        grid.set(VecUtil.d2i(getPosition()).down(), BLOCK.BLOCK);
        grid.set(VecUtil.d2i(getPosition()), BLOCK.WIRE);
        outputs.add(new Vec4i(VecUtil.d2i(getPosition()), 8));
    }

    @Override
    public void route(TriConsumer<Set<Vec4i>, Vec4i, Node> routeWire) throws RedilogPlacementException {
        if (input != null) {
            routeWire.accept(input.getOutputs(), new Vec4i(VecUtil.d2i(position), 8), input);
        }
    }

    @Override
    public Box getBoundingBox() {
        return new Box(position.add(0, -1, 0), position.add(1, 1, 1));
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes, Random rng) {
        //get average positions of inputs, outputs, and self
        Vec3d avgInputs = VecUtil.avg(input.position);
        Vec3d avgOutputs = VecUtil.avg(outputNodes.stream().map(s -> s.get()).toArray(Vec3d[]::new));
        position = VecUtil.avg(avgInputs, avgOutputs, position);
        //introduce randomness
        position = position.add(rng.nextDouble(-1, 1), rng.nextDouble(-1, 1), rng.nextDouble(-1, 1));

        //repel from other nodes
        for (Node n : otherNodes) {
            double distSqr = position.squaredDistanceTo(n.position);
            position = position.lerp(n.position, -5 / (distSqr + 1));
        }

        //snap away from other nodes
        for (Node n : otherNodes) {
            Box cur = getBoundingBox();
            Box other = n.getBoundingBox();
            if (cur.intersects(other)) {
                double dx = MathUtil.signMin(other.maxX - cur.minX, other.minX - cur.maxX);
                double dy = MathUtil.signMin(other.maxY - cur.minY, other.minY - cur.maxY);
                double dz = MathUtil.signMin(other.maxZ - cur.minZ, other.minZ - cur.maxZ);
                if (Math.abs(dx) < Math.abs(dy) && Math.abs(dx) < Math.abs(dz)) {
                    position = position.add(dx, 0, 0);
                    if (n instanceof ComponentNode) {
                        n.position = n.position.subtract(dx, 0, 0);
                    }
                } else if (Math.abs(dy) < Math.abs(dz)) {
                    position = position.add(0, dy, 0);
                    if (n instanceof ComponentNode) {
                        n.position = n.position.subtract(0, dy, 0);
                    }
                } else {
                    position = position.add(0, 0, dz);
                    if (n instanceof ComponentNode) {
                        n.position = n.position.subtract(0, 0, dz);
                    }
                }
            }
        }

        clampToBuildSpace(buildSpace);
    }

    public void clampToBuildSpace(Box buildSpace) {
        //clamp by buildspace
        double x = position.x;
        double y = position.y;
        double z = position.z;
        if (x < 0) {
            x = 0;
        } else if (x + 1 >= buildSpace.getXLength()) {
            x = buildSpace.getXLength() - 1;
        }
        if (y < 1) {
            y = 1;
        } else if (y + 1 >= buildSpace.getYLength()) {
            y = buildSpace.getYLength() - 1;
        }
        if (z < 2) {
            z = 2;
        } else if (z + 1 >= buildSpace.getZLength() - 3) {
            z = buildSpace.getZLength() - 3 - 1;
        }
        position = new Vec3d(x, y, z);
    }

    private Vec3d getPosition() {
        return position;
    }
}