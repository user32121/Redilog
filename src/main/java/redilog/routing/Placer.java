package redilog.routing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import redilog.init.Redilog;
import redilog.routing.GridLayout.Connections;
import redilog.synthesis.LogicGraph;
import redilog.synthesis.LogicGraph.Expression;
import redilog.synthesis.LogicGraph.Node;

public class Placer {
    /**
     * Place redstone according to the logic graph in the specified cuboid region
     * @param minPos minimum block position, inclusive
     * @param maxPos maximum block position, inclusive
     * @throws RedilogPlacementException
     */
    public static void placeRedilog(LogicGraph graph, BlockPos minPos, BlockPos maxPos, World world)
            throws RedilogPlacementException {
        if (minPos.getX() > maxPos.getX() || minPos.getY() > maxPos.getY() || minPos.getZ() > maxPos.getZ()) {
            throw new IllegalArgumentException(String.format("minPos %s was greater than maxPos %s", minPos, maxPos));
        }
        //adjust so there is room for signs
        minPos = minPos.add(0, 0, 1);

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

        //TODO new algo

        //place components randomly

        //link wires using greedy

        //reduce

        GridLayout grid = new GridLayout((maxPos.getX() - minPos.getX()) / 2, (maxPos.getY() - minPos.getY()) / 5,
                (maxPos.getZ() - minPos.getZ()) / 2);
        Map<Node, Vec3i> placedNodes = new HashMap<>();
        {
            //place all inputs (braces for scoping)
            int x = 0;
            for (Entry<String, Node> input : graph.inputs.entrySet()) {
                if (x >= grid.grid.length) {
                    throw new RedilogPlacementException(
                            String.format("Insufficient space for inputs (ran out at %s)", input.getKey()));
                }
                grid.grid[x][0][0] = new Connections();
                placedNodes.put(input.getValue(), new Vec3i(x, 0, 0));
                ++x;
            }
        }
        {
            //place and route rest of nodes
            Queue<Entry<String, Node>> toProcess = new LinkedList<>();
            toProcess.addAll(graph.nodes.entrySet());
            toProcess.removeAll(graph.inputs.entrySet());

            Entry<String, Node> queueMarker = null;
            while (!toProcess.isEmpty()) {
                //detect infinite loop
                if (queueMarker == null) {
                    queueMarker = toProcess.peek();
                } else if (queueMarker == toProcess.peek()) {
                    throw new RedilogPlacementException(
                            String.format("infinite loop detected for \"%s\" while placing", queueMarker.getKey()));
                }

                Entry<String, Node> entry = toProcess.remove();
                Expression source = entry.getValue().value;
                if (source == null) {
                    Redilog.LOGGER.warn("non-input \"{}\" has no value", entry.getKey());
                    continue;
                }
                Vec3i sourcePos = placedNodes.get(source);
                if (sourcePos == null) {
                    toProcess.add(entry);
                    continue;
                }
                queueMarker = null;

                if (grid.grid[sourcePos.getX()][sourcePos.getY()][sourcePos.getZ() + 2] != null) {
                    //access value from branch
                    grid.grid[sourcePos.getX()][sourcePos.getY()][sourcePos.getZ() + 1].posY = true;
                    grid.grid[sourcePos.getX()][sourcePos.getY() + 1][sourcePos.getZ() + 1] = new Connections();
                    int xDelta = 0;
                    while (grid.grid[sourcePos.getX() + xDelta][sourcePos.getY()][sourcePos.getZ() + 1] != null) {
                        ++xDelta;
                        grid.grid[sourcePos.getX() + xDelta - 1][sourcePos.getY() + 1][sourcePos.getZ()
                                + 1].posX = true;
                        grid.grid[sourcePos.getX() + xDelta][sourcePos.getY() + 1][sourcePos.getZ()
                                + 1] = new Connections();
                    }
                    grid.grid[sourcePos.getX() + xDelta][sourcePos.getY() + 1][sourcePos.getZ() + 1].negY = true;
                    grid.grid[sourcePos.getX() + xDelta][sourcePos.getY()][sourcePos.getZ() + 1] = new Connections();
                    grid.grid[sourcePos.getX() + xDelta][sourcePos.getY()][sourcePos.getZ() + 1].posZ = true;
                    grid.grid[sourcePos.getX() + xDelta][sourcePos.getY()][sourcePos.getZ() + 2] = new Connections();
                    placedNodes.put(entry.getValue(), sourcePos.add(xDelta, 0, 2));
                } else {
                    //first node, access value directly after
                    grid.grid[sourcePos.getX()][sourcePos.getY()][sourcePos.getZ()].posZ = true;
                    grid.grid[sourcePos.getX()][sourcePos.getY()][sourcePos.getZ() + 1] = new Connections();
                    grid.grid[sourcePos.getX()][sourcePos.getY()][sourcePos.getZ() + 1].posZ = true;
                    grid.grid[sourcePos.getX()][sourcePos.getY()][sourcePos.getZ() + 2] = new Connections();
                    placedNodes.put(entry.getValue(), sourcePos.add(0, 0, 2));
                }
            }
        }

        for (int x = 0; x < grid.grid.length; x++) {
            for (int y = 0; y < grid.grid[x].length; y++) {
                for (int z = 0; z < grid.grid[x][y].length; z++) {
                    if (grid.grid[x][y][z] != null) {
                        wire(world, minPos.add(x * 2, y * 5, z * 2));
                        if (grid.grid[x][y][z].posX) {
                            repeater(world, minPos.add(x * 2 + 1, y * 5, z * 2), Direction.WEST);
                        }
                        if (grid.grid[x][y][z].negX) {
                            repeater(world, minPos.add(x * 2 - 1, y * 5, z * 2), Direction.EAST);
                        }
                        if (grid.grid[x][y][z].posY) {
                            concrete(world, minPos.add(x * 2, y * 5, z * 2));
                            world.setBlockState(minPos.add(x * 2, y * 5 + 1, z * 2),
                                    Blocks.REDSTONE_TORCH.getDefaultState());
                            concrete(world, minPos.add(x * 2, y * 5 + 2, z * 2));
                            world.setBlockState(minPos.add(x * 2, y * 5 + 3, z * 2),
                                    Blocks.REDSTONE_TORCH.getDefaultState());
                            concrete(world, minPos.add(x * 2, y * 5 + 4, z * 2));
                        }
                        if (grid.grid[x][y][z].negY) {
                            world.setBlockState(minPos.add(x * 2, y * 5 - 2, z * 2),
                                    Blocks.STICKY_PISTON.getDefaultState().with(PistonBlock.FACING, Direction.DOWN));
                            world.setBlockState(minPos.add(x * 2, y * 5 - 3, z * 2),
                                    Blocks.REDSTONE_BLOCK.getDefaultState());
                            world.setBlockState(minPos.add(x * 2, y * 5 - 4, z * 2),
                                    Blocks.AIR.getDefaultState());
                        }
                        if (grid.grid[x][y][z].posZ) {
                            repeater(world, minPos.add(x * 2, y * 5, z * 2 + 1), Direction.NORTH);
                        }
                        if (grid.grid[x][y][z].negZ) {
                            repeater(world, minPos.add(x * 2, y * 5, z * 2 - 1), Direction.SOUTH);
                        }
                    }
                }
            }
        }

        for (Entry<String, Node> entry : graph.inputs.entrySet()) {
            Vec3i pos = placedNodes.get(entry.getValue());
            if (pos == null) {
                Redilog.LOGGER.warn("Failed to label input {}", entry.getKey());
                continue;
            }
            world.setBlockState(minPos.add(pos.multiply(2)), Blocks.WHITE_CONCRETE.getDefaultState());
            world.setBlockState(minPos.add(pos.multiply(2)).add(0, 1, 0),
                    Blocks.LEVER.getDefaultState().with(LeverBlock.FACE, WallMountLocation.FLOOR));
            world.setBlockState(minPos.add(pos.multiply(2)).add(0, 0, -1),
                    Blocks.BIRCH_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, Direction.NORTH));
            if (world.getBlockEntity(minPos.add(pos.multiply(2)).add(0, 0, -1)) instanceof SignBlockEntity sbe) {
                sbe.setTextOnRow(0, Text.of(entry.getKey()));
            }
        }
        for (Entry<String, Node> entry : graph.outputs.entrySet()) {
            Vec3i pos = placedNodes.get(entry.getValue());
            if (pos == null) {
                Redilog.LOGGER.warn("Failed to label output {}", entry.getKey());
                continue;
            }
            world.setBlockState(minPos.add(pos.multiply(2)), Blocks.REDSTONE_LAMP.getDefaultState());
            world.setBlockState(minPos.add(pos.multiply(2)).add(0, 1, 0), Blocks.BIRCH_SIGN.getDefaultState());
            if (world.getBlockEntity(minPos.add(pos.multiply(2)).add(0, 1, 0)) instanceof SignBlockEntity sbe) {
                sbe.setTextOnRow(0, Text.of(entry.getKey()));
            }
        }
    }

    //shorthands to reduce line length
    private static void repeater(World world, BlockPos pos, Direction dir) {
        concrete(world, pos.add(0, -1, 0));
        world.setBlockState(pos, Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, dir));
    }

    private static void wire(World world, BlockPos pos) {
        concrete(world, pos.add(0, -1, 0));
        world.setBlockState(pos, Blocks.REDSTONE_WIRE.getDefaultState());
    }

    private static void concrete(World world, BlockPos pos) {
        world.setBlockState(pos, Blocks.LIGHT_BLUE_CONCRETE.getDefaultState());
    }
}
