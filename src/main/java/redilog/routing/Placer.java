package redilog.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.block.Blocks;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import redilog.init.Redilog;
import redilog.routing.GridLayout.Connections;
import redilog.synthesis.LogicGraph;
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

        //TODO implement
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

        GridLayout grid = new GridLayout((maxPos.getX() - minPos.getX()) / 2, (maxPos.getY() - minPos.getY()) / 2,
                (maxPos.getZ() - minPos.getZ()) / 2);
        Map<String, Vec3i> namedNodes = new HashMap<>();
        {
            int x = 0;
            for (Entry<String, Node> input : graph.inputs.entrySet()) {
                if (x >= grid.grid.length) {
                    throw new RedilogPlacementException(
                            String.format("Insufficient space for inputs (ran out at %s)", input.getKey()));
                }
                grid.grid[x][0][0] = new Connections();
                grid.grid[x][0][0].posZ = true;
                namedNodes.put(input.getKey(), new Vec3i(x, 0, 0));
                ++x;
            }
        }
        //TODO add other nodes and connect

        for (int x = 0; x < grid.grid.length; x++) {
            for (int y = 0; y < grid.grid[x].length; y++) {
                for (int z = 0; z < grid.grid[x][y].length; z++) {
                    if (grid.grid[x][y][z] != null) {
                        world.setBlockState(minPos.add(x * 2, y * 2, z * 2), Blocks.REDSTONE_WIRE.getDefaultState());
                        if (grid.grid[x][y][z].posX) {
                            world.setBlockState(minPos.add(x * 2 + 1, y * 2, z * 2),
                                    Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.WEST));
                        }
                        if (grid.grid[x][y][z].negX) {
                            world.setBlockState(minPos.add(x * 2 - 1, y * 2, z * 2),
                                    Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.EAST));
                        }
                        if (grid.grid[x][y][z].posY) {
                            //TODO torch ladder
                        }
                        if (grid.grid[x][y][z].negY) {
                            //TODO downward piston
                        }
                        if (grid.grid[x][y][z].posZ) {
                            world.setBlockState(minPos.add(x * 2, y * 2, z * 2 + 1),
                                    Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.NORTH));
                        }
                        if (grid.grid[x][y][z].negZ) {
                            world.setBlockState(minPos.add(x * 2, y * 2, z * 2 - 1),
                                    Blocks.REPEATER.getDefaultState().with(RepeaterBlock.FACING, Direction.SOUTH));
                        }
                    }
                }
            }
        }

        //TODO label inputs and outputs
    }
}
