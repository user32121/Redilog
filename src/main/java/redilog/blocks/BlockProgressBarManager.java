package redilog.blocks;

import net.minecraft.entity.boss.BossBarManager;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class BlockProgressBarManager {

    private String blockName;
    private BlockPos pos;
    private BossBarManager bbm;

    public BlockProgressBarManager(String blockName, BlockPos pos, BossBarManager bossBarManager) {
        this.blockName = blockName;
        this.pos = pos;
        this.bbm = bossBarManager;
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
        if (bbm.get(id) == null) {
            bbm.add(id, Text.of(String.format(
                    "%s (%d,%d,%d) %s", blockName, pos.getX(), pos.getY(), pos.getZ(), name)));
        }
        return bbm.get(id);
    }

}
