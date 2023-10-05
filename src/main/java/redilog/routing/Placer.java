package redilog.routing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.NotImplementedException;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import redilog.init.Redilog;
import redilog.routing.bfs.BFSStep;
import redilog.synthesis.InputNode;
import redilog.synthesis.LogicGraph;
import redilog.synthesis.Node;
import redilog.synthesis.OrNode;
import redilog.synthesis.OutputNode;
import redilog.utils.Array3D;
import redilog.utils.Array3DView;
import redilog.utils.Array4D;
import redilog.utils.Vec4i;

public class Placer {
    public enum BLOCK {
        AIR,
        STRICT_AIR, //a block that must be air, such as above diagonal wires
        WIRE,
        BLOCK,
        REPEATER_NORTH,
        REPEATER_SOUTH,
        REPEATER_EAST,
        REPEATER_WEST,
    }

    /**
     * Place redstone according to the logic graph in the specified cuboid region
     * @param feedback the function will add messages that should be relayed to the user
     * @throws RedilogPlacementException
     */
    public static void placeRedilog(LogicGraph graph, Box buildSpace, World world, Consumer<Text> feedback)
            throws RedilogPlacementException {
        if (buildSpace == null || (buildSpace.getAverageSideLength() == 0)) {
            throw new RedilogPlacementException(
                    "No build space specified (specify by creating a zone using layout markers, then placing the builder next to one of the markers)");
        }

        Map<Node, String> symbolNames = new HashMap<>();
        for (Map.Entry<String, Node> entry : graph.expressions.entrySet()) {
            symbolNames.put(entry.getValue(), entry.getKey());
        }
        // Redilog.LOGGER.info("inputs: " + graph.inputs.size());
        Redilog.LOGGER.info("outputs: " + graph.inputs.size());
        Redilog.LOGGER.info("all nodes: " + graph.expressions.size());

        Array3D<BLOCK> grid = new Array3D.Builder<BLOCK>()
                .size((int) buildSpace.getXLength(), (int) buildSpace.getYLength(), (int) buildSpace.getZLength())
                .fill(BLOCK.AIR).build();
        Map<Node, WireDescriptor> wires = new LinkedHashMap<>(); //use linkedhashmap to have a deterministic iteration order
        for (Entry<String, Node> entry : graph.expressions.entrySet()) {
            WireDescriptor wd = new WireDescriptor();
            wd.isDebug = entry.getKey().contains("DEBUG");
            wires.put(entry.getValue(), wd);
        }

        placeIO(buildSpace, graph, grid, wires, feedback);
        placeComponents(buildSpace, grid, wires);
        //view prevents routing wires in sign space
        routeWires(new Array3DView<>(grid, 0, 0, 2, grid.getXLength(), grid.getYLength(), grid.getZLength() - 1),
                wires, feedback);
        transferGridToWorld(buildSpace, world, grid);
        labelIO(buildSpace, graph, world, wires, feedback);
        //TODO repeat while adjusting buildSpace and layout to fine tune
    }

    private static void transferGridToWorld(Box buildSpace, World world, Array3D<BLOCK> grid) {
        BlockPos minPos = new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ);
        for (int x = 0; x < buildSpace.getXLength(); ++x) {
            for (int y = 0; y < buildSpace.getYLength(); ++y) {
                for (int z = 0; z < buildSpace.getZLength(); ++z) {
                    BlockState state = switch (grid.get(x, y, z)) {
                        case AIR -> Blocks.AIR.getDefaultState();
                        case STRICT_AIR -> Blocks.AIR.getDefaultState();
                        case WIRE -> Blocks.REDSTONE_WIRE.getDefaultState();
                        case BLOCK -> Blocks.LIGHT_BLUE_CONCRETE.getDefaultState();
                        case REPEATER_NORTH ->
                            Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.SOUTH);
                        case REPEATER_SOUTH ->
                            Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.NORTH);
                        case REPEATER_EAST ->
                            Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.WEST);
                        case REPEATER_WEST ->
                            Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.EAST);
                        default -> throw new NotImplementedException(grid.get(x, y, z) + " not implemented");
                    };
                    if (state != null) {
                        world.setBlockState(minPos.add(x, y, z), state);
                    }
                }
            }
        }
    }

    private static void placeComponents(Box buildSpace, Array3D<BLOCK> grid, Map<Node, WireDescriptor> wires) {
        //TODO make better deterministic (maybe supply seed as parameter?)
        Random rng = new Random(100);
        //check for components that are not placed
        for (Entry<Node, WireDescriptor> entry : wires.entrySet()) {
            if (entry.getValue().source == null) {
                if (entry.getKey() instanceof OrNode on) {
                    Vec3i pos = new Vec3i(rng.nextInt((int) buildSpace.getXLength() - 1), 0,
                            rng.nextInt((int) buildSpace.getZLength() - 3));
                    entry.getValue().source = pos.add(1, 1, 0);
                    for (int x = 0; x < 3; x++) {
                        entry.getValue().wires.add(new Vec4i(pos.add(x, 1, 2), 15));
                    }
                    BLOCK[][][] orGateBlocks = {
                            { { BLOCK.BLOCK, BLOCK.BLOCK, BLOCK.BLOCK },
                                    { BLOCK.WIRE, BLOCK.REPEATER_SOUTH, BLOCK.WIRE }, },
                            { { BLOCK.AIR, BLOCK.AIR, BLOCK.BLOCK },
                                    { BLOCK.AIR, BLOCK.AIR, BLOCK.WIRE }, },
                            { { BLOCK.BLOCK, BLOCK.BLOCK, BLOCK.BLOCK },
                                    { BLOCK.WIRE, BLOCK.REPEATER_SOUTH, BLOCK.WIRE }, }, };
                    for (BlockPos offset : BlockPos.iterate(0, 0, 0,
                            orGateBlocks.length - 1, orGateBlocks[0].length - 1, orGateBlocks[0][0].length - 1)) {
                        grid.set(pos.add(offset), orGateBlocks[offset.getX()][offset.getY()][offset.getZ()]);
                    }
                } else {
                    throw new NotImplementedException(entry.getKey().getClass() + " not implemented");
                }
            }
        }
    }

    private static void routeWires(Array3D<BLOCK> grid, Map<Node, WireDescriptor> wires, Consumer<Text> feedback) {
        for (Entry<Node, WireDescriptor> entry : wires.entrySet()) {
            if (entry.getKey() instanceof InputNode in) {
                //NO OP
            } else if (entry.getKey() instanceof OutputNode on) {
                if (on.value != null) {
                    routeBFS(entry.getValue().source, wires.get(on.value).wires, grid, wires, entry.getValue().isDebug,
                            feedback);
                }
            } else if (entry.getKey() instanceof OrNode on) {
                if (on.input1 != null) {
                    routeBFS(entry.getValue().source.add(-1, 0, 0), wires.get(on.input1).wires, grid, wires,
                            entry.getValue().isDebug, feedback);
                }
                if (on.input2 != null) {
                    routeBFS(entry.getValue().source.add(1, 0, 0), wires.get(on.input2).wires, grid, wires,
                            entry.getValue().isDebug, feedback);
                }
            } else {
                throw new NotImplementedException(entry.getKey().getClass() + " not implemented");
            }
        }
    }

    //constructs a path from one of starts to end
    private static void routeBFS(Vec3i end, Set<Vec4i> starts, Array3D<BLOCK> grid, Map<Node, WireDescriptor> wires,
            boolean isDebug, Consumer<Text> feedback) {
        //(4th dimension represents signal strength)
        Queue<Vec4i> toProcess = new LinkedList<>();
        Array4D<Vec4i> visitedFrom = new Array4D.Builder<Vec4i>().size(new Vec4i(grid.getSize(), 16)).build();
        Array4D<Integer> cost = new Array4D.Builder<Integer>().size(new Vec4i(grid.getSize(), 16))
                .fill(Integer.MAX_VALUE).build();
        Array4D<BFSStep> wireType = new Array4D.Builder<BFSStep>().size(new Vec4i(grid.getSize(), 16)).build();

        //NOTE: since bfs stores limited state, it may be possible for the wire to loop on itself and override its path
        for (Vec4i pos : starts) {
            toProcess.add(pos);
            cost.set(pos, 0);
        }
        while (!toProcess.isEmpty()) {
            Vec4i cur = toProcess.remove();
            for (BFSStep step : BFSStep.STEPS) {
                Vec4i move = step.getValidMove(grid, cur, end);
                if (move != null && cost.inBounds(move) && cost.get(cur) + step.getCost() < cost.get(move)) {
                    visitedFrom.set(move, cur);
                    cost.set(move, cost.get(cur) + step.getCost());
                    wireType.set(move, step);
                    if (move.getW() > 0) { //blocks with 0 power don't need to be explored
                        toProcess.add(move);
                    }
                }
            }
        }

        //check if path exists
        int bestPathCost = Integer.MAX_VALUE;
        int bestPathEnd = -1;
        for (int i = 1; i < 16; ++i) {
            if (cost.get(new Vec4i(end, i)) < bestPathCost) {
                bestPathCost = cost.get(new Vec4i(end, i));
                bestPathEnd = i;
            }
        }
        if (bestPathEnd == -1) {
            //TODO provide names of nodes
            logErrorAndCreateMessage(feedback, String.format("unable to path %s to %s", starts, end));
        } else {
            //trace path
            Vec4i cur = visitedFrom.get(new Vec4i(end, bestPathEnd));
            while (!starts.contains(cur)) {
                if (isDebug) {
                    Redilog.LOGGER.info("{}", cur);
                }
                Vec4i[] placeds = wireType.get(cur).place(visitedFrom.get(cur), grid);
                for (Vec4i placed : placeds) {
                    starts.add(placed);
                }
                cur = visitedFrom.get(cur);
            }
        }
    }

    private static void placeIO(Box buildSpace, LogicGraph graph, Array3D<BLOCK> grid,
            Map<Node, WireDescriptor> wires, Consumer<Text> feedback) throws RedilogPlacementException {
        if (buildSpace.getZLength() < 3) {
            throw new RedilogPlacementException("Not enough space for I/O. Need z length >= 3.");
        } else if (buildSpace.getYLength() < 2) {
            throw new RedilogPlacementException("Not enough space for I/O. Need height >= 2.");
        }
        if (buildSpace.getZLength() < 5) {
            logWarnAndCreateMessage(feedback,
                    "Limited space for I/O; potentially degenerate layout. Recommended z length >= 5.");
        }
        if (buildSpace.getXLength() < Math.max(graph.inputs.size(), graph.outputs.size()) * 2 - 1) {
            logWarnAndCreateMessage(feedback,
                    String.format("Limited space for I/O; potentially degenerate layout. Recommended x length >= %s.",
                            Math.max(graph.inputs.size(), graph.outputs.size()) * 2 - 1));
        }
        //place inputs and outputs evenly spaced along x
        //inputs
        Iterator<Entry<String, InputNode>> inputs = graph.inputs.entrySet().stream()
                .sorted((lhs, rhs) -> lhs.getKey().compareTo(rhs.getKey())).iterator();
        for (int i = 0; i < graph.inputs.size(); ++i) {
            InputNode input = inputs.next().getValue();
            int x = (int) (i * (buildSpace.getXLength() - 1) / (graph.inputs.size() - 1));
            grid.set(x, 0, 1, BLOCK.BLOCK);
            grid.set(x, 0, 2, BLOCK.BLOCK);
            grid.set(x, 1, 2, BLOCK.WIRE);
            wires.get(input).source = new Vec3i(x, 1, 1);
            wires.get(input).wires.add(new Vec4i(x, 1, 2, 15));
        }
        //outputs
        Iterator<Entry<String, OutputNode>> outputs = graph.outputs.entrySet().stream()
                .sorted((lhs, rhs) -> lhs.getKey().compareTo(rhs.getKey())).iterator();
        for (int i = 0; i < graph.outputs.size(); ++i) {
            int x = (int) (i * (buildSpace.getXLength() - 1) / (graph.outputs.size() - 1));
            OutputNode output = outputs.next().getValue();
            int z = grid.getZLength() - 2;
            grid.set(x, 1, z, BLOCK.WIRE);
            wires.get(output).source = new Vec3i(x, 1, z);
        }
    }

    private static void labelIO(Box buildSpace, LogicGraph graph, World world,
            Map<Node, WireDescriptor> wires, Consumer<Text> feedback) {
        BlockPos minPos = new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ);
        for (Entry<String, InputNode> entry : graph.inputs.entrySet()) {
            Vec3i pos = wires.get(entry.getValue()).source;
            if (pos == null) {
                logWarnAndCreateMessage(feedback, String.format("Failed to label input %s", entry.getKey()));
                continue;
            }
            world.setBlockState(minPos.add(pos.down()), Blocks.WHITE_CONCRETE.getDefaultState());
            world.setBlockState(minPos.add(pos),
                    Blocks.LEVER.getDefaultState().with(LeverBlock.FACE, WallMountLocation.FLOOR));
            world.setBlockState(minPos.add(pos).add(0, -1, -1),
                    Blocks.BIRCH_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, Direction.NORTH));
            if (world.getBlockEntity(minPos.add(pos).add(0, -1, -1)) instanceof SignBlockEntity sbe) {
                sbe.setTextOnRow(0, Text.of(entry.getKey()));
            }
        }
        for (Entry<String, OutputNode> entry : graph.outputs.entrySet()) {
            Vec3i pos = wires.get(entry.getValue()).source;
            if (pos == null) {
                logWarnAndCreateMessage(feedback, String.format("Failed to label output %s", entry.getKey()));
                continue;
            }
            world.setBlockState(minPos.add(pos.down()), Blocks.REDSTONE_LAMP.getDefaultState());
            world.setBlockState(minPos.add(pos), Blocks.REDSTONE_WIRE.getDefaultState());
            world.setBlockState(minPos.add(pos).add(0, -1, 1),
                    Blocks.BIRCH_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, Direction.SOUTH));
            if (world.getBlockEntity(minPos.add(pos).add(0, -1, 1)) instanceof SignBlockEntity sbe) {
                sbe.setTextOnRow(0, Text.of(entry.getKey()));
            }
        }
    }

    private static void logWarnAndCreateMessage(Consumer<Text> feedback, String message) {
        Redilog.LOGGER.warn(message);
        feedback.accept(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
    }

    private static void logErrorAndCreateMessage(Consumer<Text> feedback, String message) {
        Redilog.LOGGER.error(message);
        feedback.accept(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.RED)));
    }
}
