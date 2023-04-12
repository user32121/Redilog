package redilog.routing;

import net.minecraft.world.World;
import redilog.init.Redilog;
import redilog.synthesis.Graph;

public class Placer {
    public static void placeRedilog(Graph graph, World world) throws RedilogPlacementException {
        //TODO implement
        Redilog.LOGGER.info("inputs:");
        for (var entry : graph.inputs.entrySet()) {
            Redilog.LOGGER.info("{} ({}): {} = {}", entry.getKey(), entry.getValue().range, entry.getValue(),
                    entry.getValue().value);
        }
        Redilog.LOGGER.info("outputs:");
        for (var entry : graph.outputs.entrySet()) {
            Redilog.LOGGER.info("{} ({}): {} = {}", entry.getKey(), entry.getValue().range, entry.getValue(),
                    entry.getValue().value);
        }
        Redilog.LOGGER.info("nodes:");
        for (var entry : graph.nodes.entrySet()) {
            Redilog.LOGGER.info("{} ({}): {} = {}", entry.getKey(), entry.getValue().range, entry.getValue(),
                    entry.getValue().value);
        }
    }
}
