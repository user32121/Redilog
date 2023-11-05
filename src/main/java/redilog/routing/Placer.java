package redilog.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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
import redilog.utils.LoggerUtil;
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
        for (Map.Entry<String, Node> entry : graph.nodes.entrySet()) {
            symbolNames.put(entry.getValue(), entry.getKey());
        }
        Redilog.LOGGER.info("node count: " + graph.nodes.size());

        Array3D<BLOCK> grid = new Array3D.Builder<BLOCK>()
                .size((int) buildSpace.getXLength(), (int) buildSpace.getYLength(), (int) buildSpace.getZLength())
                .fill(BLOCK.AIR).build();

        //TODO logging substeps
        placeIO(buildSpace, graph, feedback);
        placeComponents(buildSpace, grid, graph);
        //view prevents routing wires in sign space
        routeWires(new Array3DView<>(grid, 0, 0, 2, grid.getXLength(), grid.getYLength(), grid.getZLength() - 1),
                graph, feedback);
        transferGridToWorld(buildSpace, world, grid);
        labelIO(buildSpace, graph, world, feedback);
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

    private static void placeComponents(Box buildSpace, Array3D<BLOCK> grid, LogicGraph graph) {
        //TODO make better deterministic (maybe supply seed as parameter?)
        Random rng = new Random(100);
        //check for nodes that are not placed
        //give a random initial position
        for (Node node : graph.nodes.values()) {
            Vec3d pos = new Vec3d(rng.nextInt((int) buildSpace.getXLength()),
                    rng.nextInt((int) buildSpace.getYLength()),
                    rng.nextInt((int) buildSpace.getZLength()));
            node.potentialPosition = pos;
        }
        //repeatedly adjust so they are close to their target
        //TODO pick less arbitrary repeat constant
        for (int i = 0; i < 10; i++) {
            for (Node node : graph.nodes.values()) {
                node.adjustPotentialPosition(buildSpace, graph.nodes.values());
            }
        }
        for (Node node : graph.nodes.values()) {
            node.placeAtPotentialPos(grid);
        }
    }

    private static void routeWires(Array3D<BLOCK> grid, LogicGraph graph, Consumer<Text> feedback)
            throws RedilogPlacementException {
        //TODO delegate more
        for (Node node : graph.nodes.values()) {
            if (node instanceof InputNode in) {
                //NO OP
            } else if (node instanceof OutputNode on) {
                if (on.value != null) {
                    routeBFS(on.value.getOutputs(), on.position, grid, graph, on.value, on, feedback);
                }
            } else if (node instanceof OrNode on) {
                if (on.input1 != null) {
                    routeBFS(on.input1.getOutputs(), on.getInput1(), grid, graph, on.input1, on, feedback);
                }
                if (on.input2 != null) {
                    routeBFS(on.input2.getOutputs(), on.getInput2(), grid, graph, on.input2, on, feedback);
                }
            } else {
                throw new NotImplementedException(node.getClass() + " not implemented");
            }
        }
    }

    //constructs a path from one of starts to end
    private static void routeBFS(Set<Vec4i> starts, Vec3i end, Array3D<BLOCK> grid, LogicGraph graph,
            Node startNode, Node endNode, Consumer<Text> feedback) {
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
            LoggerUtil.logErrorAndCreateMessage(feedback,
                    String.format("unable to path %s to %s (%s to %s)",
                            startNode.owner.declaration, endNode.owner.declaration, starts, end),
                    String.format("unable to path %s to %s",
                            startNode.owner.declaration, endNode.owner.declaration));
        } else {
            //trace path
            Vec4i cur = visitedFrom.get(new Vec4i(end, bestPathEnd));
            while (!starts.contains(cur)) {
                if (endNode.isDebug()) {
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

    private static void placeIO(Box buildSpace, LogicGraph graph, Consumer<Text> feedback)
            throws RedilogPlacementException {
        if (buildSpace.getZLength() < 3) {
            throw new RedilogPlacementException("Not enough space for I/O. Need z length >= 3.");
        } else if (buildSpace.getYLength() < 2) {
            throw new RedilogPlacementException("Not enough space for I/O. Need height >= 2.");
        }
        if (buildSpace.getZLength() < 5) {
            LoggerUtil.logWarnAndCreateMessage(feedback,
                    "Limited space for I/O; potentially degenerate layout. Recommended z length >= 5.");
        }
        List<InputNode> inputs = new ArrayList<>();
        List<OutputNode> outputs = new ArrayList<>();
        for (Node n : graph.nodes.values()) {
            if (n instanceof InputNode in) {
                inputs.add(in);
            } else if (n instanceof OutputNode on) {
                outputs.add(on);
            }
        }
        if (buildSpace.getXLength() < Math.max(inputs.size(), outputs.size()) * 2 - 1) {
            LoggerUtil.logWarnAndCreateMessage(feedback,
                    String.format("Limited space for I/O; potentially degenerate layout. Recommended x length >= %s.",
                            Math.max(inputs.size(), outputs.size()) * 2 - 1));
        }
        //place inputs and outputs evenly spaced along x
        //inputs
        Collections.sort(inputs, (l, r) -> l.name.compareTo(r.name));
        for (int i = 0; i < inputs.size(); ++i) {
            InputNode input = inputs.get(i);
            int x = (int) (i * (buildSpace.getXLength() - 1) / (inputs.size() - 1));
            input.position = new Vec3i(x, 1, 1);
        }
        //outputs
        Collections.sort(outputs, (l, r) -> l.name.compareTo(r.name));
        for (int i = 0; i < outputs.size(); ++i) {
            OutputNode output = outputs.get(i);
            int x = (int) (i * (buildSpace.getXLength() - 1) / (outputs.size() - 1));
            int z = (int) (buildSpace.getZLength() - 2);
            output.position = new Vec3i(x, 1, z);
        }
    }

    private static void labelIO(Box buildSpace, LogicGraph graph, World world, Consumer<Text> feedback) {
        BlockPos minPos = new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ);
        for (Entry<String, Node> entry : graph.nodes.entrySet()) {
            //TODO delegate
            if (entry.getValue() instanceof InputNode in) {
                Vec3i pos = in.position;
                if (pos == null) {
                    LoggerUtil.logWarnAndCreateMessage(feedback,
                            String.format("Failed to label input %s", entry.getKey()));
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
            } else if (entry.getValue() instanceof OutputNode on) {
                Vec3i pos = on.position;
                if (pos == null) {
                    LoggerUtil.logWarnAndCreateMessage(feedback,
                            String.format("Failed to label output %s", entry.getKey()));
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
    }
}
