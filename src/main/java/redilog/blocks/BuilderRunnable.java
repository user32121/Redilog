package redilog.blocks;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import redilog.init.Redilog;
import redilog.init.RedilogGamerules;
import redilog.parsing.Parser;
import redilog.parsing.RedilogParsingException;
import redilog.parsing.SymbolGraph;
import redilog.routing.Placer;
import redilog.routing.Placer.BLOCK;
import redilog.routing.RedilogPlacementException;
import redilog.synthesis.LogicGraph;
import redilog.synthesis.RedilogSynthesisException;
import redilog.synthesis.Synthesizer;
import redilog.utils.Array3D;

public class BuilderRunnable implements Runnable {
    public final BuilderBlockEntity owner;
    public final ServerPlayerEntity player;
    public final String redilog;
    public final Box buildSpace;
    public final World world;

    public volatile boolean shouldStop = false;
    public volatile LogicGraph lGraph;
    public volatile Array3D<BLOCK> blocks;
    public volatile Iterator<BlockPos> poss;

    public BuilderRunnable(BuilderBlockEntity owner, ServerPlayerEntity player, String redilog, Box buildSpace,
            World world) {
        this.owner = owner;
        this.player = player;
        this.redilog = redilog;
        this.buildSpace = buildSpace;
        this.world = world;
    }

    @Override
    public void run() {
        try {
            BlockProgressBarManager bbpbm = new BlockProgressBarManager("builder", owner.getPos(), player);

            if (shouldStop) {
                return;
            }
            Redilog.LOGGER.info("Begin parsing stage");
            player.sendMessage(Text.of("Parsing..."));
            SymbolGraph sGraph = Parser.parseRedilog(redilog, player::sendMessage, bbpbm);
            Redilog.LOGGER.info("Begin synthesize stage");
            if (shouldStop) {
                return;
            }
            player.sendMessage(Text.of("Synthesizing..."));
            lGraph = Synthesizer.synthesize(sGraph, player::sendMessage, bbpbm);
            if (shouldStop) {
                return;
            }
            Redilog.LOGGER.info("Begin placing and routing stage");
            player.sendMessage(Text.of("Placing..."));
            blocks = Placer.placeAndRoute(lGraph, buildSpace, player::sendMessage, bbpbm, world, this::getShouldStop);
            poss = BlockPos.iterate(BlockPos.ORIGIN, new BlockPos(blocks.getSize().add(-1, -1, -1))).iterator();
            if (shouldStop) {
                return;
            }
            player.sendMessage(Text.of("  Transferring to world..."));
        } catch (RedilogParsingException e) {
            player.sendMessage(Text.literal("An error occurred during parsing.\n")
                    .append(Text.literal(e.toString()).setStyle(Style.EMPTY.withColor(Formatting.RED))));
            Redilog.LOGGER.error("An error occurred during parsing", e);
        } catch (RedilogSynthesisException e) {
            player.sendMessage(Text.literal("An error occurred during synthesis.\n")
                    .append(Text.literal(e.toString()).setStyle(Style.EMPTY.withColor(Formatting.RED))));
            Redilog.LOGGER.error("An error occurred during synthesis", e);
        } catch (RedilogPlacementException e) {
            player.sendMessage(Text.literal("An error occurred during placement.\n")
                    .append(Text.literal(e.toString()).setStyle(Style.EMPTY.withColor(Formatting.RED))));
            Redilog.LOGGER.error("An error occurred during placement", e);
        } catch (Exception e) {
            player.sendMessage(Text.of("An internal error occurred. See server log for more details."));
            Redilog.LOGGER.error("An internal error occurred", e);
        }
    }

    /**
     * @return false if there are more operations to be performed.
     * (e.g. This can occur if it takes took long to place all blocks)
     */
    public boolean mainThreadOperations() {
        try {
            if (poss == null) {
                return true;
            }
            if (poss.hasNext()) {
                Stopwatch sw = Stopwatch.createStarted();
                while (sw.elapsed(TimeUnit.MILLISECONDS) < world.getGameRules()
                        .getInt(RedilogGamerules.MS_TRANSFERRING_PER_TICK) && this.poss.hasNext()) {
                    BlockPos curPos = poss.next();
                    Placer.transferGridToWorld(buildSpace, world, blocks, curPos);
                }
                if (poss.hasNext()) {
                    return false;
                }
            }
            Placer.labelIO(buildSpace, lGraph, world, player::sendMessage);
            player.sendMessage(Text.of("Build finished."));
        } catch (Exception e) {
            player.sendMessage(Text.of("An internal error occurred. See server log for more details."));
            Redilog.LOGGER.error("An internal error occurred", e);
        }
        return true;
    }

    public boolean getShouldStop() {
        return shouldStop;
    }
}
