package redilog.routing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import redilog.init.Redilog;
import redilog.synthesis.LogicGraph;
import redilog.synthesis.LogicGraph.Expression;
import redilog.synthesis.LogicGraph.Node;
import redilog.utils.Array3D;
import redilog.utils.ConstantSupplier;

public class Placer {
    private enum BLOCK {
        AIR,
        WIRE,
        BLOCK,
    }

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

        Array3D<BLOCK> grid = new Array3D<>((int) buildSpace.getXLength(), (int) buildSpace.getYLength(),
                (int) buildSpace.getZLength(), new ConstantSupplier<>(BLOCK.AIR));
        Map<Expression, WireDescriptor> nodes = new HashMap<>();
        for (Node node : graph.nodes.values()) {
            nodes.put(node, new WireDescriptor());
        }

        placeIO(buildSpace, graph, grid, nodes, world.random);
        placeNodes();
        routeWires(grid, nodes);
        sliceWires();
        transferGridToWorld(buildSpace, world, grid);
        labelIO(buildSpace, graph, world, nodes);
        warnUnused(graph);
        //TODO attempt to place even if an error occurs
    }

    private static void warnUnused(LogicGraph graph) {
        //TODO implement
    }

    private static void transferGridToWorld(Box buildSpace, World world, Array3D<BLOCK> grid) {
        BlockPos minPos = new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ);
        for (int x = 0; x < buildSpace.getXLength(); ++x) {
            for (int y = 0; y < buildSpace.getYLength(); ++y) {
                for (int z = 0; z < buildSpace.getZLength(); ++z) {
                    ItemPlacementContext ipc = new AutomaticItemPlacementContext(world, minPos.add(x, y, z),
                            Direction.DOWN, ItemStack.EMPTY, Direction.UP);
                    BlockState state = switch (grid.get(x, y, z)) {
                        case AIR -> Blocks.AIR.getDefaultState();
                        case WIRE -> Blocks.REDSTONE_WIRE.getPlacementState(ipc);
                        case BLOCK -> Blocks.LIGHT_BLUE_CONCRETE.getPlacementState(ipc);
                        default -> throw new NotImplementedException(grid.get(x, y, z) + " not implemented");
                    };
                    world.setBlockState(minPos.add(x, y, z), state);
                }
            }
        }
    }

    private static void placeNodes() {
        //TODO for each node, if not placed (wireDesc.input==null), place in random pos
    }

    private static final Vec3i[] WIRE_DIRS = new Vec3i[] {
            new Vec3i(0, -1, 1), new Vec3i(0, -1, -1),
            new Vec3i(0, 0, 1), new Vec3i(0, 0, -1),
            new Vec3i(0, 1, 1), new Vec3i(0, 1, -1),
            new Vec3i(1, -1, 0), new Vec3i(-1, -1, 0),
            new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0),
            new Vec3i(1, 1, 0), new Vec3i(-1, 1, 0), };
    private static final Vec3i DOWN = new Vec3i(0, -1, 0);

    private static void routeWires(Array3D<BLOCK> grid, Map<Expression, WireDescriptor> graphNodes) {
        for (Entry<Expression, WireDescriptor> entry : graphNodes.entrySet()) {
            if (entry.getKey() instanceof Node node) {
                if (node.value == null) {
                    continue;
                }
                Vec3i end = entry.getValue().input;
                Vec3i start = graphNodes.get(node.value).wires.stream()
                        .min(((v1, v2) -> (end.getManhattanDistance(v1) - end.getManhattanDistance(v2))))
                        .orElseGet(() -> entry.getValue().input);

                //bfs
                Queue<Vec3i> toProcess = new LinkedList<>();
                Array3D<Vec3i> visitedFrom = new Array3D<>(grid.getSize());
                //NOTE: since bfs does not store state, it may be possible for the wire to loop on itself and override its path,
                //      but this is unlikely to occur considering a loop would be longer than the original path
                toProcess.add(start);
                while (!toProcess.isEmpty()) {
                    Vec3i cur = toProcess.remove();
                    for (Vec3i dir : WIRE_DIRS) {
                        if ((grid.isValue(cur.add(dir), BLOCK.AIR) && grid.isValue(cur.add(dir).add(DOWN), BLOCK.AIR)
                                || cur.add(dir).equals(end))
                                && visitedFrom.isValue(cur.add(dir), null)) {
                            visitedFrom.set(cur.add(dir), cur);
                            toProcess.add(cur.add(dir));
                        }
                    }
                }

                //trace path
                if (visitedFrom.isValue(end, null)) {
                    Redilog.LOGGER.error("unable to path {} to {}", start, end);
                } else {
                    Vec3i cur = visitedFrom.get(end);
                    while (!cur.equals(start)) {
                        grid.set(cur, BLOCK.WIRE);
                        grid.set(cur.add(DOWN), BLOCK.BLOCK);
                        entry.getValue().wires.add(cur);
                        cur = visitedFrom.get(cur);
                    }
                }
            } else {
                throw new NotImplementedException(entry.getKey().getClass().getName() + " not implemented");
            }
        }
    }

    private static void sliceWires() {
        //TODO implement
        // remove slices of layout where redundant (such as reducing all x-ward wires from 4 to 3)
    }

    private static void placeIO(Box buildSpace, LogicGraph graph, Array3D<BLOCK> grid,
            Map<Expression, WireDescriptor> graphNodes, Random random) throws RedilogPlacementException {
        //place inputs and outputs evenly spaced along x
        //TODO put inputs in a sorted order
        //inputs
        Iterator<Entry<String, Node>> nodes = graph.inputs.entrySet().iterator();
        int nodesRemaining = graph.inputs.size();
        for (int x = 0; x < buildSpace.getXLength(); ++x) {
            if (random.nextDouble() < nodesRemaining / (buildSpace.getXLength() - x) * 2) {
                Entry<String, Node> node = nodes.next();
                grid.set(x, 0, 1, BLOCK.BLOCK);
                grid.set(x, 0, 2, BLOCK.BLOCK);
                grid.set(x, 1, 2, BLOCK.WIRE);
                graphNodes.get(node.getValue()).input = new Vec3i(x, 1, 1);
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
                int z = grid.getZLength() - 2;
                grid.set(x, 0, z, BLOCK.BLOCK);
                grid.set(x, 1, z, BLOCK.BLOCK);
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
            Map<Expression, WireDescriptor> nodes) {
        BlockPos minPos = new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ);
        for (Entry<String, Node> entry : graph.inputs.entrySet()) {
            Vec3i pos = nodes.get(entry.getValue()).input;
            if (pos == null) {
                Redilog.LOGGER.warn("Failed to label input {}", entry.getKey());
                continue;
            }
            world.setBlockState(minPos.add(pos.add(0, -1, 0)), Blocks.WHITE_CONCRETE.getDefaultState());
            world.setBlockState(minPos.add(pos),
                    Blocks.LEVER.getDefaultState().with(LeverBlock.FACE, WallMountLocation.FLOOR));
            world.setBlockState(minPos.add(pos).add(0, -1, -1),
                    Blocks.BIRCH_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, Direction.NORTH));
            if (world.getBlockEntity(minPos.add(pos).add(0, -1, -1)) instanceof SignBlockEntity sbe) {
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
