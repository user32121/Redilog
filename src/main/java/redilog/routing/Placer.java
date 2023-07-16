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
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import redilog.init.Redilog;
import redilog.synthesis.LogicGraph;
import redilog.synthesis.LogicGraph.Expression;
import redilog.synthesis.LogicGraph.InputExpression;
import redilog.synthesis.LogicGraph.OutputExpression;
import redilog.utils.Array3D;
import redilog.utils.ConstantSupplier;

public class Placer {
    private enum BLOCK {
        AIR,
        WIRE,
        BLOCK,
    }

    //locations a wire can travel to
    private static final Vec3i[] WIRE_DIRS;
    //locations that must be air for corresponding wire in WIRE_DIRS to be valid (relative to WIRE_DIR)
    private static final Vec3i[][] REQUIRED_AIR;

    static {
        WIRE_DIRS = new Vec3i[12];
        REQUIRED_AIR = new Vec3i[12][];

        //posz
        WIRE_DIRS[0] = new Vec3i(0, 0, 1);
        REQUIRED_AIR[0] = new Vec3i[] { new Vec3i(0, 0, 0), new Vec3i(0, -1, 0),
                new Vec3i(0, -1, 1), new Vec3i(0, 0, 1),
                new Vec3i(1, -1, 0), new Vec3i(1, 0, 0),
                new Vec3i(-1, -1, 0), new Vec3i(-1, 0, 0), };
        WIRE_DIRS[1] = new Vec3i(0, -1, 1);
        REQUIRED_AIR[1] = REQUIRED_AIR[0];
        WIRE_DIRS[2] = new Vec3i(0, 1, 1);
        REQUIRED_AIR[2] = REQUIRED_AIR[0];
        //negz
        WIRE_DIRS[3] = new Vec3i(0, 0, -1);
        REQUIRED_AIR[3] = new Vec3i[] { new Vec3i(0, 0, 0), new Vec3i(0, -1, 0),
                new Vec3i(0, -1, -1), new Vec3i(0, 0, -1),
                new Vec3i(1, -1, 0), new Vec3i(1, 0, 0),
                new Vec3i(-1, -1, 0), new Vec3i(-1, 0, 0), };
        WIRE_DIRS[4] = new Vec3i(0, -1, -1);
        REQUIRED_AIR[4] = REQUIRED_AIR[3];
        WIRE_DIRS[5] = new Vec3i(0, 1, -1);
        REQUIRED_AIR[5] = REQUIRED_AIR[3];
        //posx
        WIRE_DIRS[6] = new Vec3i(1, 0, 0);
        REQUIRED_AIR[6] = new Vec3i[] { new Vec3i(0, 0, 0), new Vec3i(0, -1, 0),
                new Vec3i(1, -1, 0), new Vec3i(1, 0, 0),
                new Vec3i(0, -1, 1), new Vec3i(0, 0, 1),
                new Vec3i(0, -1, -1), new Vec3i(0, 0, -1), };
        WIRE_DIRS[7] = new Vec3i(1, -1, 0);
        REQUIRED_AIR[7] = REQUIRED_AIR[6];
        WIRE_DIRS[8] = new Vec3i(1, 1, 0);
        REQUIRED_AIR[8] = REQUIRED_AIR[6];
        //negx
        WIRE_DIRS[9] = new Vec3i(-1, 0, 0);
        REQUIRED_AIR[9] = new Vec3i[] { new Vec3i(0, 0, 0), new Vec3i(0, -1, 0),
                new Vec3i(-1, -1, 0), new Vec3i(-1, 0, 0),
                new Vec3i(0, -1, 1), new Vec3i(0, 0, 1),
                new Vec3i(0, -1, -1), new Vec3i(0, 0, -1), };
        WIRE_DIRS[10] = new Vec3i(-1, -1, 0);
        REQUIRED_AIR[10] = REQUIRED_AIR[9];
        WIRE_DIRS[11] = new Vec3i(-1, 1, 0);
        REQUIRED_AIR[11] = REQUIRED_AIR[9];
    }

    private static final Vec3i DOWN = new Vec3i(0, -1, 0);

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
            Redilog.LOGGER.info("{}: {}", entry.getKey(), entry.getValue());
        }
        Redilog.LOGGER.info("outputs:");
        for (var entry : graph.outputs.entrySet()) {
            Redilog.LOGGER.info("{}: {} = {}", entry.getKey(), entry.getValue(), entry.getValue().value);
        }
        Redilog.LOGGER.info("expressions:");
        for (var entry : graph.expressions.entrySet()) {
            if (entry.getValue() instanceof InputExpression ie) {
                Redilog.LOGGER.info("{}: {}", entry.getKey(), ie);
            } else if (entry.getValue() instanceof OutputExpression oe) {
                Redilog.LOGGER.info("{}: {} = {}", entry.getKey(), oe, oe.value);
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
        routeWires(grid, wires);
        sliceWires();
        transferGridToWorld(buildSpace, world, grid);
        labelIO(buildSpace, graph, world, wires);
        warnUnused(graph);
    }

    private static void warnUnused(LogicGraph graph) {
        //TODO implement
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
                Vec3i start = wires.get(oe.value).wires.stream()
                        .min(((v1, v2) -> (end.getManhattanDistance(v1) - end.getManhattanDistance(v2))))
                        .orElseGet(() -> entry.getValue().source);

                //bfs
                Queue<Vec3i> toProcess = new LinkedList<>();
                Array3D<Vec3i> visitedFrom = new Array3D<>(grid.getSize());
                //NOTE: since bfs does not store state, it may be possible for the wire to loop on itself and override its path,
                //      but this is unlikely to occur considering a loop would be longer than the original path
                toProcess.add(start);
                while (!toProcess.isEmpty()) {
                    Vec3i cur = toProcess.remove();
                    for (int i = 0; i < WIRE_DIRS.length; ++i) {
                        Vec3i dir = WIRE_DIRS[i];
                        boolean valid = true;
                        if (!cur.add(dir).equals(end)) {
                            for (Vec3i dir2 : REQUIRED_AIR[i]) {
                                if (cur.add(dir).add(dir2).equals(end)) {
                                    continue;
                                }
                                if (!grid.isValue(cur.add(dir).add(dir2), BLOCK.AIR)) {
                                    valid = false;
                                }
                            }
                        }
                        if (valid && visitedFrom.isValue(cur.add(dir), null) && grid.inBounds(cur.add(dir).add(DOWN))) {
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
                        wires.get(oe.value).wires.add(cur);
                        cur = visitedFrom.get(cur);
                    }
                }
            } else {
                throw new NotImplementedException(entry.getKey().getClass() + " not implemented");
            }
        }
    }

    private static void sliceWires() {
        //TODO implement
        // remove slices of layout where redundant (such as reducing all x-ward wires from 4 to 3)
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
