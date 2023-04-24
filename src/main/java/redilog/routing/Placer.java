package redilog.routing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import redilog.init.Redilog;
import redilog.routing.GridLayout.BLOCK;
import redilog.synthesis.LogicGraph;
import redilog.synthesis.LogicGraph.Node;

public class Placer {
    /**
     * Place redstone according to the logic graph in the specified cuboid region
     * @throws RedilogPlacementException
     */
    public static void placeRedilog(LogicGraph graph, Box buildSpace, World world)
            throws RedilogPlacementException {
        if (buildSpace == null || (buildSpace.getAverageSideLength() == 0)) {
            throw new RedilogPlacementException(
                    "No build space specified (specify by creating a zone using layout markers, then placing the builder next to one of the markers)");
        }

        Redilog.LOGGER.info("inputs:");
        for (var entry : graph.inputs.entrySet()) {
            Redilog.LOGGER.info("{}: {} = {}", entry.getKey(), entry.getValue(), entry.getValue().value);
        }
        Redilog.LOGGER.info("outputs:");
        for (var entry : graph.outputs.entrySet()) {
            Redilog.LOGGER.info("{}: {} = {}", entry.getKey(), entry.getValue(), entry.getValue().value);
        }
        Redilog.LOGGER.info("nodes:");
        for (var entry : graph.nodes.entrySet()) {
            Redilog.LOGGER.info("{}: {} = {}", entry.getKey(), entry.getValue(), entry.getValue().value);
        }

        GridLayout grid = new GridLayout((int) buildSpace.getXLength(), (int) buildSpace.getYLength(),
                (int) buildSpace.getZLength());
        Map<Node, WireDescriptor> nodes = new HashMap<>();
        for (Node node : graph.nodes.values()) {
            nodes.put(node, new WireDescriptor());
        }

        placeIO(buildSpace, graph, grid, nodes, world.random);
        placeNodes();
        routeWires();
        sliceWires();
        transferGridToWorld(buildSpace, world, grid);
        labelIO(buildSpace, graph, world, nodes);
    }

    private static void transferGridToWorld(Box buildSpace, World world, GridLayout grid) {
        BlockPos minPos = new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ);
        for (int x = 0; x < buildSpace.getXLength(); ++x) {
            for (int y = 0; y < buildSpace.getYLength(); ++y) {
                for (int z = 0; z < buildSpace.getZLength(); ++z) {
                    BlockState state = Blocks.AIR.getDefaultState();
                    switch (grid.grid[x][y][z]) {
                        case AIR:
                            break;
                        case BLOCK:
                            state = Blocks.LIGHT_BLUE_CONCRETE.getDefaultState();
                            break;
                        case WIRE:
                            state = Blocks.REDSTONE_WIRE.getDefaultState();
                            break;
                        default:
                            throw new NotImplementedException(grid.grid[x][y][z] + " not implemented");
                    }
                    world.setBlockState(minPos.add(x, y, z), state);
                }
            }
        }
    }

    private static void placeNodes() {
        //TODO for each node, if not placed (wireDesc.input==null), place in random pos
    }

    private static void routeWires() {
        //TODO for each node, try to place a wire from input to input block
    }

    private static void sliceWires() {
        //TODO implement
        // remove slices of layout where redundant (such as reducing all x-ward wires from 4 to 3)
    }

    private static void placeIO(Box buildSpace, LogicGraph graph, GridLayout grid,
            Map<Node, WireDescriptor> graphNodes, Random random) throws RedilogPlacementException {
        //place inputs and outputs evenly spaced along x
        //TODO put inputs in a sorted order
        //inputs
        Iterator<Entry<String, Node>> nodes = graph.inputs.entrySet().iterator();
        int nodesRemaining = graph.inputs.size();
        for (int x = 0; x < buildSpace.getXLength(); ++x) {
            if (random.nextDouble() < nodesRemaining / (buildSpace.getXLength() - x) * 2) {
                Entry<String, Node> node = nodes.next();
                grid.grid[x][0][1] = BLOCK.BLOCK;
                grid.grid[x][0][2] = BLOCK.BLOCK;
                grid.grid[x][1][2] = BLOCK.WIRE;
                graphNodes.get(node.getValue()).input = new Vec3i(x, 0, 1);
                graphNodes.get(node.getValue()).wires.add(new Vec3i(x, 1, 2));
                --nodesRemaining;
                ++x;
                if (!nodes.hasNext()) {
                    break;
                }
            }
        }
        if (nodes.hasNext()) {
            throw new RedilogPlacementException(
                    String.format("Insufficient space for inputs (ran out at %s)", nodes.next().getKey()));
        }
        //outputs
        nodes = graph.outputs.entrySet().iterator();
        nodesRemaining = graph.outputs.size();
        for (int x = 0; x < buildSpace.getXLength(); ++x) {
            if (random.nextDouble() < nodesRemaining / (buildSpace.getXLength() - x) * 2) {
                Entry<String, Node> node = nodes.next();
                int z = grid.grid[x][1].length - 2;
                grid.grid[x][0][z] = BLOCK.BLOCK;
                grid.grid[x][1][z] = BLOCK.BLOCK;
                graphNodes.get(node.getValue()).input = new Vec3i(x, 1, z);
                --nodesRemaining;
                ++x;
                if (!nodes.hasNext()) {
                    break;
                }
            }
        }
        if (nodes.hasNext()) {
            throw new RedilogPlacementException(
                    String.format("Insufficient space for outputs (ran out at %s)", nodes.next().getKey()));
        }
    }

    private static void labelIO(Box buildSpace, LogicGraph graph, World world,
            Map<Node, WireDescriptor> nodes) {
        BlockPos minPos = new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ);
        for (Entry<String, Node> entry : graph.inputs.entrySet()) {
            Vec3i pos = nodes.get(entry.getValue()).input;
            if (pos == null) {
                Redilog.LOGGER.warn("Failed to label input {}", entry.getKey());
                continue;
            }
            world.setBlockState(minPos.add(pos), Blocks.WHITE_CONCRETE.getDefaultState());
            world.setBlockState(minPos.add(pos).add(0, 1, 0),
                    Blocks.LEVER.getDefaultState().with(LeverBlock.FACE, WallMountLocation.FLOOR));
            world.setBlockState(minPos.add(pos).add(0, 0, -1),
                    Blocks.BIRCH_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, Direction.NORTH));
            if (world.getBlockEntity(minPos.add(pos).add(0, 0, -1)) instanceof SignBlockEntity sbe) {
                sbe.setTextOnRow(0, Text.of(entry.getKey()));
            }
        }
        for (Entry<String, Node> entry : graph.outputs.entrySet()) {
            Vec3i pos = nodes.get(entry.getValue()).input;
            if (pos == null) {
                Redilog.LOGGER.warn("Failed to label output {}", entry.getKey());
                continue;
            }
            world.setBlockState(minPos.add(pos), Blocks.REDSTONE_LAMP.getDefaultState());
            world.setBlockState(minPos.add(pos).add(0, 0, 1),
                    Blocks.BIRCH_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, Direction.SOUTH));
            if (world.getBlockEntity(minPos.add(pos).add(0, 0, 1)) instanceof SignBlockEntity sbe) {
                sbe.setTextOnRow(0, Text.of(entry.getKey()));
            }
        }
    }
}
