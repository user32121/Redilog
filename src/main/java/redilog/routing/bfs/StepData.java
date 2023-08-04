package redilog.routing.bfs;

import redilog.routing.Placer.BLOCK;
import redilog.utils.Vec4i;

public class StepData {
    /**
     * 4th coordinate indicates signal strength
     */
    public Vec4i pos;
    public BLOCK type;

    public StepData(Vec4i pos, BLOCK type) {
        this.pos = pos;
        this.type = type;
    }
}
