package redilog.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import redilog.blocks.BlockProgressBarManager;
import redilog.init.Redilog;
import redilog.init.RedilogGamerules;
import redilog.routing.steps.RoutingStep;
import redilog.synthesis.LogicGraph;
import redilog.synthesis.nodes.IONode;
import redilog.synthesis.nodes.InputNode;
import redilog.synthesis.nodes.Node;
import redilog.synthesis.nodes.OutputNode;
import redilog.utils.Array3D;
import redilog.utils.Array3DView;
import redilog.utils.Array4D;
import redilog.utils.LoggerUtil;
import redilog.utils.Vec4i;
import redilog.utils.VecUtil;

public class Placer {
    /**
     * Place redstone according to the logic graph in the specified cuboid region
     * @param feedback the function will add messages that should be relayed to the user
     * @param bbpbm
     * @return 
     * @throws RedilogPlacementException
     */
    public static Array3D<BLOCK> placeAndRoute(LogicGraph graph, Box buildSpace, Consumer<Text> feedback,
            BlockProgressBarManager bbpbm, World world)
            throws RedilogPlacementException {
        if (buildSpace == null || (buildSpace.getAverageSideLength() == 0)) {
            throw new RedilogPlacementException(
                    "No build space specified (specify by creating a zone using layout markers, then placing the builder next to one of the markers)");
        }

        Redilog.LOGGER.info("node count: " + graph.nodes.size());

        Array3D<BLOCK> grid = new Array3D.Builder<BLOCK>()
                .size((int) buildSpace.getXLength(), (int) buildSpace.getYLength(), (int) buildSpace.getZLength())
                .fill(BLOCK.AIR).build();

        if (Thread.currentThread().isInterrupted()) {
            return grid;
        }
        feedback.accept(Text.of("  Placing IO..."));
        placeIO(buildSpace, graph, feedback);
        //TODO repeat while adjusting buildSpace and layout to fine tune
        if (Thread.currentThread().isInterrupted()) {
            return grid;
        }
        feedback.accept(Text.of("  Placing components..."));
        placeComponents(buildSpace, grid, graph, bbpbm, world);
        if (Thread.currentThread().isInterrupted()) {
            return grid;
        }
        feedback.accept(Text.of("  Routing wires..."));
        //view prevents routing wires in sign space
        routeWires(new Array3DView<>(grid, 0, 0, 2, grid.getXLength(), grid.getYLength(), grid.getZLength() - 1),
                graph, feedback, bbpbm);
        return grid;
    }

    public static void transferGridToWorld(Box buildSpace, World world, Array3D<BLOCK> grid, BlockPos pos) {
        BlockPos minPos = new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ);
        world.setBlockState(minPos.add(pos), grid.get(pos).states[0], Block.NOTIFY_LISTENERS);
    }

    private static void placeComponents(Box buildSpace, Array3D<BLOCK> grid, LogicGraph graph,
            BlockProgressBarManager bbpbm, World world) {
        Random rng = new Random();
        //check for nodes that are not placed
        //give a random initial position
        for (Node node : graph.nodes) {
            Vec3d pos = new Vec3d(rng.nextInt((int) buildSpace.getXLength()),
                    rng.nextInt((int) buildSpace.getYLength()),
                    rng.nextInt((int) buildSpace.getZLength()));
            node.setPotentialPosition(pos);
        }
        CommandBossBar cbb = bbpbm.getProgressBar("placing");
        cbb.setMaxValue(world.getGameRules().getInt(RedilogGamerules.PLACEMENT_GRAPH_ITERATIONS));
        cbb.setValue(0);
        //repeatedly adjust so they are close to their target
        for (int i = 0; i < world.getGameRules().getInt(RedilogGamerules.PLACEMENT_GRAPH_ITERATIONS); i++) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            for (Node node : graph.nodes) {
                node.adjustPotentialPosition(buildSpace, graph.nodes, rng);
            }
            cbb.setValue(i + 1);
        }
        bbpbm.finishProgressBar(cbb);
        for (Node node : graph.nodes) {
            node.placeAtPotentialPos(grid, buildSpace);
        }
    }

    private static void routeWires(Array3D<BLOCK> grid, LogicGraph graph, Consumer<Text> feedback,
            BlockProgressBarManager bbpbm)
            throws RedilogPlacementException {
        CommandBossBar cbb = bbpbm.getProgressBar("routing");
        cbb.setMaxValue(graph.nodes.size());
        cbb.setValue(0);
        for (Node node : graph.nodes) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            node.route((starts, end, startNode) -> routeWire(starts, end, grid, graph, startNode, node, feedback));
            cbb.setValue(cbb.getValue() + 1);
        }
        bbpbm.finishProgressBar(cbb);
    }

    /**
     * Constructs a path from one of starts to end.
     * @param starts any allowable starting position
     * @param end target of routing; w component indicates <i>minimum</i> signal strength needed
     */
    //TODO sometimes fails to route even though a clear path exists (maybe detects its own wires as obstacles?)
    private static void routeWire(Set<Vec4i> starts, Vec4i end, Array3D<BLOCK> grid, LogicGraph graph,
            Node startNode, Node endNode, Consumer<Text> feedback) {
        //(4th dimension represents signal strength)
        Array4D<Vec4i> visitedFrom = new Array4D.Builder<Vec4i>().size(new Vec4i(grid.getSize(), 16)).build();
        Array4D<Integer> cost = new Array4D.Builder<Integer>().size(new Vec4i(grid.getSize(), 16))
                .fill(Integer.MAX_VALUE).build();
        Queue<Vec4i> toProcess = new PriorityQueue<>(
                (v1, v2) -> cost.get(v1) + 10 * v1.to3i().getManhattanDistance(end.to3i())
                        - cost.get(v2) - 10 * v2.to3i().getManhattanDistance(end.to3i()));
        Array4D<RoutingStep> wireType = new Array4D.Builder<RoutingStep>().size(new Vec4i(grid.getSize(), 16)).build();

        //NOTE: since routing stores limited state, it may be possible for the wire to loop on itself and override its path
        for (Vec4i pos : starts) {
            toProcess.add(pos);
            cost.set(pos, 0);
        }
        boolean terminate = false;
        while (!toProcess.isEmpty() && !terminate) {
            Vec4i cur = toProcess.remove();
            for (RoutingStep step : RoutingStep.STEPS) {
                Vec4i move = step.getValidMove(grid, cur, end.to3i(), wireType.get(cur));
                if (move != null && cost.inBounds(move) && cost.get(cur) + step.getCost() < cost.get(move)) {
                    visitedFrom.set(move, cur);
                    cost.set(move, cost.get(cur) + step.getCost());
                    wireType.set(move, step);
                    if (move.getW() > 0) { //blocks with 0 power don't need to be explored
                        toProcess.add(move);
                    }
                    if (end.to3i().equals(move.to3i())) {
                        //end reached, check if any paths potentially have lower cost
                        Vec4i bestPath = toProcess.peek();
                        if (cost.get(bestPath) + 10 * bestPath.to3i().getManhattanDistance(end.to3i()) >= cost
                                .get(move)) {
                            //found optimal path, terminate
                            terminate = true;
                        }
                    }
                }
            }
        }

        //check if path exists
        int bestPathCost = Integer.MAX_VALUE;
        int bestPathEnd = -1;
        for (int i = end.getW(); i < 16; ++i) {
            if (cost.get(new Vec4i(end.to3i(), i)) < bestPathCost) {
                bestPathCost = cost.get(new Vec4i(end.to3i(), i));
                bestPathEnd = i;
            }
        }
        if (bestPathEnd == -1) {
            LoggerUtil.logErrorAndCreateMessage(feedback,
                    String.format("unable to path %s to %s (%s to %s)",
                            startNode.owner.nodeAsString(startNode), endNode.owner.nodeAsString(endNode), starts, end),
                    String.format("unable to path %s to %s",
                            startNode.owner.nodeAsString(startNode), endNode.owner.nodeAsString(endNode)));
        } else {
            //trace path
            Vec4i cur = new Vec4i(end.to3i(), bestPathEnd);
            while (!starts.contains(cur)) {
                if (endNode.isDebug()) {
                    Redilog.LOGGER.info("{}", cur);
                }
                //TODO wireType sometimes has a null in it, but inconsistent
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
        for (Node n : graph.nodes) {
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
            input.setPosition(new Vec3d(x, 1, 1));
        }
        //outputs
        Collections.sort(outputs, (l, r) -> l.name.compareTo(r.name));
        for (int i = 0; i < outputs.size(); ++i) {
            OutputNode output = outputs.get(i);
            int x = (int) (i * (buildSpace.getXLength() - 1) / (outputs.size() - 1));
            int z = (int) (buildSpace.getZLength() - 2);
            output.setPosition(VecUtil.i2d(new Vec3i(x, 1, z)));
        }
    }

    public static void labelIO(Box buildSpace, LogicGraph graph, World world, Consumer<Text> feedback) {
        BlockPos relativeOrigin = new BlockPos(buildSpace.minX, buildSpace.minY, buildSpace.minZ);
        for (Node node : graph.nodes) {
            if (node instanceof IONode ion) {
                ion.placeLabel(world, relativeOrigin, feedback);
            }
        }
    }
}
