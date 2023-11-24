package redilog.init;

import net.fabricmc.fabric.api.gamerule.v1.CustomGameRuleCategory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;

public class RedilogGamerules {
    public static final CustomGameRuleCategory REDILOG_GAMERULE = new CustomGameRuleCategory(
            new Identifier("redilog", "gamerule.category"), Text.of("Redilog"));

    //number of milliseconds spent per tick transferring to world
    public static final GameRules.Key<GameRules.IntRule> MS_TRANSFERRING_PER_TICK = GameRuleRegistry.register(
            "msTransferringPerTick", REDILOG_GAMERULE, GameRuleFactory.createIntRule(1, 0));
    //iterations to simulate graph physics
    public static final GameRules.Key<GameRules.IntRule> PLACEMENT_GRAPH_ITERATIONS = GameRuleRegistry.register(
            "placementGraphIterations", REDILOG_GAMERULE, GameRuleFactory.createIntRule(100, 0));

    public static void init() {
    }
}
