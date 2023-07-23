package redilog.routing;

import java.util.HashMap;
import java.util.Iterator;
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
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import redilog.init.Redilog;
import redilog.routing.bfs.BFSStep;
import redilog.synthesis.LogicGraph;
import redilog.synthesis.LogicGraph.Expression;
import redilog.synthesis.LogicGraph.InputExpression;
import redilog.synthesis.LogicGraph.OutputExpression;
import redilog.utils.Array3D;
import redilog.utils.Array3DView;
import redilog.utils.ConstantSupplier;

public class Placer {
    public enum BLOCK {
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

        Array3D<BLOCK> grid = new Array3D<>((int) buildSpace.getXLength(), (int) buildSpace.getYLength(),
                (int) buildSpace.getZLength(), new ConstantSupplier<>(BLOCK.AIR));
        Map<Expression, WireDescriptor> wires = new HashMap<>();
        for (Expression expression : graph.expressions.values()) {
            wires.put(expression, new WireDescriptor());
        }

        placeIO(buildSpace, graph, grid, wires);
        placeComponents();
        //view prevents routing wires in sign space
        routeWires(new Array3DView<>(grid,
                0, 0, 2, grid.getXLength(), grid.getYLength(), grid.getZLength() - 1), wires);
        transferGridToWorld(buildSpace, world, grid);
        labelIO(buildSpace, graph, world, wires);
        warnUnused(graph, wires);
        //TODO repeat while adjusting buildSpace and layout to fine tune
    }

    private static void warnUnused(LogicGraph graph, Map<Expression, WireDescriptor> wires) {
        for (Entry<String, Expression> entry : graph.expressions.entrySet()) {
            if (entry.getValue() instanceof InputExpression) {
                if (wires.get(entry.getValue()).wires.size() <= 1) {
                    Redilog.LOGGER.warn("Unused input {}", entry.getKey());
                }
            } else if (entry.getValue() instanceof OutputExpression oe) {
                if (oe.value == null) {
                    Redilog.LOGGER.warn("Unassigned output {}", entry.getKey());
                }
            } else {
                Redilog.LOGGER.warn(String.format("%s not implemented", entry.getValue().getClass()));
            }
        }
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

    private static void routeWires(Array3D<BLOCK> grid, Map<Expression, WireDescriptor> wires) {
        for (Entry<Expression, WireDescriptor> entry : wires.entrySet()) {
            if (entry.getKey() instanceof InputExpression) {
                //NO OP
            } else if (entry.getKey() instanceof OutputExpression oe) {
                if (oe.value == null) {
                    continue;
                }
                Vec3i end = entry.getValue().source;
                Set<Vec3i> starts = wires.get(oe.value).wires;

                //bfs
                Queue<Vec3i> toProcess = new LinkedList<>();
                Array3D<Vec3i> visitedFrom = new Array3D<>(grid.getSize());
                Array3D<Integer> cost = new Array3D<>(grid.getXLength(), grid.getYLength(), grid.getZLength(),
                        Integer.MAX_VALUE);
                //NOTE: since bfs does not store state, it may be possible for the wire to loop on itself and override its path
                toProcess.addAll(starts);
                starts.forEach(pos -> cost.set(pos, 0));
                while (!toProcess.isEmpty()) {
                    Vec3i cur = toProcess.remove();
                    for (BFSStep step : BFSStep.STEPS) {
                        List<Vec3i[]> validMoves = step.getValidMoves(grid, cur, end);
                        for (Vec3i[] moves : validMoves) {
                            Vec3i prev = cur;
                            for (Vec3i move : moves) {
                                if (cost.inBounds(move) && cost.get(cur) + step.getCost() < cost.get(move)) {
                                    visitedFrom.set(move, prev);
                                    cost.set(move, cost.get(cur) + step.getCost());
                                    toProcess.add(move);
                                }
                                prev = move;
                            }
                        }
                    }
                }

                //trace path
                if (visitedFrom.isValue(end, null)) {
                    Redilog.LOGGER.error("unable to path {} to {}", starts, end);
                } else {
                    Vec3i cur = visitedFrom.get(end);
                    while (!starts.contains(cur)) {
                        grid.set(cur, BLOCK.WIRE);
                        grid.set(cur.add(0, -1, 0), BLOCK.BLOCK);
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
            Map<Expression, WireDescriptor> wires) throws RedilogPlacementException {
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
            wires.get(input).wires.add(new Vec3i(x, 1, 2));
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
            Map<Expression, WireDescriptor> wires) {
        BlockPos minPos = new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ);
        for (Entry<String, InputExpression> entry : graph.inputs.entrySet()) {
            Vec3i pos = wires.get(entry.getValue()).source;
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
        for (Entry<String, OutputExpression> entry : graph.outputs.entrySet()) {
            Vec3i pos = wires.get(entry.getValue()).source;
            if (pos == null) {
                Redilog.LOGGER.warn("Failed to label output {}", entry.getKey());
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
}
