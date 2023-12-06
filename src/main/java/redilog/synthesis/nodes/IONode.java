package redilog.synthesis.nodes;

import java.util.Collection;
import java.util.Random;
import java.util.function.Consumer;

import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import redilog.parsing.expressions.Expression;

public abstract class IONode extends Node {
    public final String name;

    public IONode(Expression owner, String name) {
        super(owner);
        this.name = name;
    }

    @Override
    public void adjustPotentialPosition(Box buildSpace, Collection<Node> otherNodes, Random rng) {
        //NO OP
    }

    @Override
    public void setPotentialPosition(Vec3d pos) {
        //NO OP
    }

    public void setPosition(Vec3d pos) {
        position = pos;
    }

    public Vec3d getPosition() {
        return position;
    }

    public abstract void placeLabel(World world, BlockPos relativeOrigin, Consumer<Text> feedback);

}
