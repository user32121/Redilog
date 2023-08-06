package redilog.routing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

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
import redilog.routing.bfs.StepData;
import redilog.synthesis.LogicGraph;
import redilog.synthesis.LogicGraph.Expression;
import redilog.synthesis.LogicGraph.InputExpression;
import redilog.synthesis.LogicGraph.OutputExpression;
import redilog.utils.Array3D;
import redilog.utils.Array3DView;
import redilog.utils.Array4D;
import redilog.utils.Vec4i;

public class Placer {
    public enum BLOCK {
        AIR,
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
    public static void placeRedilog(LogicGraph graph, Box buildSpace, World world, List<Text> feedback)
            throws RedilogPlacementException {
        if (buildSpace == null || (buildSpace.getAverageSideLength() == 0)) {
            throw new RedilogPlacementException(
                    "No build space specified (specify by creating a zone using layout markers, then placing the builder next to one of the markers)");
        }

        Map<Expression, String> symbolNames = new HashMap<>();
        for (Map.Entry<String, Expression> entry : graph.expressions.entrySet()) {
            symbolNames.put(entry.getValue(), entry.getKey());
        }
        Redilog.LOGGER.info("inputs:");
        for (var entry : graph.inputs.entrySet()) {
            Redilog.LOGGER.info("{}", entry.getKey());
        }
        Redilog.LOGGER.info("outputs:");
        for (var entry : graph.outputs.entrySet()) {
            Redilog.LOGGER.info("{} = {}", entry.getKey(), symbolNames.get(entry.getValue().value));
        }
        Redilog.LOGGER.info("other expressions:");
        for (var entry : graph.expressions.entrySet()) {
            if (entry.getValue() instanceof InputExpression ie || entry.getValue() instanceof OutputExpression oe) {
                //NO OP
            } else {
                throw new NotImplementedException(entry.getValue().getClass() + " not implemented");
            }
        }

        Array3D<BLOCK> grid = new Array3D.Builder<BLOCK>()
                .size((int) buildSpace.getXLength(), (int) buildSpace.getYLength(), (int) buildSpace.getZLength())
                .fill(BLOCK.AIR).build();
        Map<Expression, WireDescriptor> wires = new LinkedHashMap<>(); //use linkedhashmap to have a deterministic iteration order
        for (Expression expression : graph.expressions.values()) {
            wires.put(expression, new WireDescriptor());
        }

        placeIO(buildSpace, graph, grid, wires, feedback);
        placeComponents();
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

    private static void placeComponents() {
        //TODO for each component, if not placed (wireDesc.input==null), place in random pos
    }

    private static void routeWires(Array3D<BLOCK> grid, Map<Expression, WireDescriptor> wires, List<Text> feedback) {
        for (Entry<Expression, WireDescriptor> entry : wires.entrySet()) {
            if (entry.getKey() instanceof InputExpression) {
                //NO OP
            } else if (entry.getKey() instanceof OutputExpression oe) {
                //TODO generalize bfs (to nonoutputs)
                if (oe.value == null) {
                    continue;
                }
                Vec3i end = entry.getValue().source;
                Set<Vec4i> starts = wires.get(oe.value).wires;

                //bfs (4th dimension represents signal strength)
                Queue<Vec4i> toProcess = new LinkedList<>();
                Array4D<Vec4i> visitedFrom = new Array4D.Builder<Vec4i>().size(new Vec4i(grid.getSize(), 16)).build();
                Array4D<Integer> cost = new Array4D.Builder<Integer>().size(new Vec4i(grid.getSize(), 16))
                        .fill(Integer.MAX_VALUE).build();
                Array4D<BLOCK> wireType = new Array4D.Builder<BLOCK>().size(new Vec4i(grid.getSize(), 16))
                        .fill(BLOCK.AIR).build();

                //NOTE: since bfs stores limited state, it may be possible for the wire to loop on itself and override its path
                for (Vec4i pos : starts) {
                    toProcess.add(pos);
                    cost.set(pos, 0);
                }
                while (!toProcess.isEmpty()) {
                    Vec4i cur = toProcess.remove();
                    for (BFSStep step : BFSStep.STEPS) {
                        List<StepData[]> validMoves = step.getValidMoves(grid, cur, end);
                        for (StepData[] moves : validMoves) {
                            Vec4i prev = cur;
                            for (StepData move : moves) {
                                //TODO if any step out of bounds, then don't process it
                                if (cost.inBounds(move.pos) && cost.get(cur) + move.cost < cost.get(move.pos)) {
                                    visitedFrom.set(move.pos, prev);
                                    cost.set(move.pos, cost.get(cur) + move.cost);
                                    wireType.set(move.pos, move.type);
                                    if (move.pos.getW() > 0) { //blocks with 0 power don't need to be explored
                                        toProcess.add(move.pos);
                                    }
                                }
                                prev = move.pos;
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
                        grid.set(cur.to3i(), wireType.get(cur));
                        grid.set(cur.to3i().add(0, -1, 0), BLOCK.BLOCK);
                        wires.get(oe.value).wires.add(cur);
                        cur = visitedFrom.get(cur);
                    }
                }
            } else {
                throw new NotImplementedException(entry.getKey().getClass() + " not implemented");
            }
        }
    }

    private static void placeIO(Box buildSpace, LogicGraph graph, Array3D<BLOCK> grid,
            Map<Expression, WireDescriptor> wires, List<Text> feedback) throws RedilogPlacementException {
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
        Iterator<Entry<String, InputExpression>> inputs = graph.inputs.entrySet().stream()
                .sorted((lhs, rhs) -> lhs.getKey().compareTo(rhs.getKey())).iterator();
        for (int i = 0; i < graph.inputs.size(); ++i) {
            InputExpression input = inputs.next().getValue();
            int x = (int) (i * (buildSpace.getXLength() - 1) / (graph.inputs.size() - 1));
            grid.set(x, 0, 1, BLOCK.BLOCK);
            grid.set(x, 0, 2, BLOCK.BLOCK);
            grid.set(x, 1, 2, BLOCK.WIRE);
            wires.get(input).source = new Vec3i(x, 1, 1);
            wires.get(input).wires.add(new Vec4i(x, 1, 2, 15));
        }
        //outputs
        Iterator<Entry<String, OutputExpression>> outputs = graph.outputs.entrySet().stream()
                .sorted((lhs, rhs) -> lhs.getKey().compareTo(rhs.getKey())).iterator();
        for (int i = 0; i < graph.outputs.size(); ++i) {
            int x = (int) (i * (buildSpace.getXLength() - 1) / (graph.outputs.size() - 1));
            OutputExpression output = outputs.next().getValue();
            int z = grid.getZLength() - 2;
            grid.set(x, 1, z, BLOCK.WIRE);
            wires.get(output).source = new Vec3i(x, 1, z);
        }
    }

    private static void labelIO(Box buildSpace, LogicGraph graph, World world,
            Map<Expression, WireDescriptor> wires, List<Text> feedback) {
        BlockPos minPos = new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ);
        for (Entry<String, InputExpression> entry : graph.inputs.entrySet()) {
            Vec3i pos = wires.get(entry.getValue()).source;
            if (pos == null) {
                logWarnAndCreateMessage(feedback, String.format("Failed to label input %s", entry.getKey()));
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
        for (Entry<String, OutputExpression> entry : graph.outputs.entrySet()) {
            Vec3i pos = wires.get(entry.getValue()).source;
            if (pos == null) {
                logWarnAndCreateMessage(feedback, String.format("Failed to label output %s", entry.getKey()));
                continue;
            }
            world.setBlockState(minPos.add(pos.add(0, -1, 0)), Blocks.REDSTONE_LAMP.getDefaultState());
            world.setBlockState(minPos.add(pos), Blocks.REDSTONE_WIRE.getDefaultState());
            world.setBlockState(minPos.add(pos).add(0, -1, 1),
                    Blocks.BIRCH_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, Direction.SOUTH));
            if (world.getBlockEntity(minPos.add(pos).add(0, -1, 1)) instanceof SignBlockEntity sbe) {
                sbe.setTextOnRow(0, Text.of(entry.getKey()));
            }
        }
    }

    private static void logWarnAndCreateMessage(List<Text> feedback, String message) {
        Redilog.LOGGER.warn(message);
        feedback.add(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
    }

    private static void logErrorAndCreateMessage(List<Text> feedback, String message) {
        Redilog.LOGGER.error(message);
        feedback.add(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.RED)));
    }
}
