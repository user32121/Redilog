package redilog.blocks;

import net.minecraft.entity.boss.BossBarManager;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import redilog.init.RedilogGamerules;

//TODO auto add player
public class BlockProgressBarManager {

    private String blockName;
    private BlockPos pos;
    private ServerPlayerEntity player;

    public BlockProgressBarManager(String blockName, BlockPos pos, ServerPlayerEntity player) {
        this.blockName = blockName;
        this.pos = pos;
        this.player = player;
    }

    private Identifier getID(String name) {
        return new Identifier("redilog", String.format(
                "%s.%d_%d_%d.%s", blockName, pos.getX(), pos.getY(), pos.getZ(), name));
    }

    /**
     * Return the progress bar with id containing name. If the progressbar does not exist, creates it.
     */
    public CommandBossBar getProgressBar(String name) {
        Identifier id = getID(name);
        BossBarManager bbm = player.server.getBossBarManager();
        if (bbm.get(id) == null) {
            bbm.add(id, Text.of(String.format(
                    "%s (%d,%d,%d) %s", blockName, pos.getX(), pos.getY(), pos.getZ(), name)));
        }
        CommandBossBar cbb = bbm.get(id);
        if (player.server.getGameRules().getBoolean(RedilogGamerules.ADD_PLAYER_TO_PROGRESSBAR)) {
            cbb.addPlayer(player);
        }
        return cbb;
    }

    /**
     * Indicate that this progressbar has finished
     */
    public void finishProgressBar(CommandBossBar cbb) {
        if (player.server.getGameRules().getBoolean(RedilogGamerules.REMOVE_PLAYER_FROM_PROGRESSBAR)) {
            cbb.removePlayer(player);
        }
    }
}
