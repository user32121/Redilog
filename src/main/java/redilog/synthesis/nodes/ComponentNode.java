package redilog.synthesis.nodes;

import java.util.Collection;
import java.util.Random;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import redilog.parsing.expressions.Expression;
import redilog.routing.BLOCK;
import redilog.utils.Array3D;
import redilog.utils.MathUtil;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public abstract class ComponentNode extends Node {

    public ComponentNode(Expression owner) {
        super(owner);
    }

    public abstract Component getComponent();

    public Vec3i getInput(int index) {
        return VecUtil.d2i(position).add(getComponent().inputs.get(index));
    }

    @Override
    public Box getBoundingBox() {
        return new Box(position.subtract(VecUtil.i2d(getComponent().margin)),
                position.add(VecUtil.i2d(getComponent().blocks.getSize()))
                        .add(VecUtil.i2d(getComponent().margin)));
    }

    @Override
    public void placeAtPotentialPos(Array3D<BLOCK> grid, Box buildSpace) {
        clampToBuildSpace(buildSpace);

        for (Vec4i output : getComponent().outputs) {
            outputs.add(new Vec4i(VecUtil.d2i(position), 0).add(output));
        }
        for (BlockPos offset : BlockPos.iterate(BlockPos.ORIGIN,
                new BlockPos(getComponent().blocks.getSize().add(-1, -1, -1)))) {
            BLOCK b = getComponent().blocks.get(offset);
            if (b != null) {
                grid.set(VecUtil.d2i(position).add(offset), b);
            }
        }
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes, Random rng) {
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
        } else if (x + getComponent().blocks.getXLength() >= buildSpace.getXLength()) {
            x = buildSpace.getXLength() - getComponent().blocks.getXLength();
        }
        if (y < 0) {
            y = 0;
        } else if (y + getComponent().blocks.getYLength() >= buildSpace.getYLength()) {
            y = buildSpace.getYLength() - getComponent().blocks.getYLength();
        }
        if (z < 3) {
            z = 3;
        } else if (z + getComponent().blocks.getZLength() >= buildSpace.getZLength() - 3) {
            z = buildSpace.getZLength() - 3 - getComponent().blocks.getZLength();
        }
        position = new Vec3d(x, y, z);
    }
}
