package redilog.routing;

import net.minecraft.util.math.Vec3i;

/**
 * A simplified representation of the world that should be easier to route in
 */
public class GridLayout {
    public enum BLOCK {
        AIR,
        WIRE,
        BLOCK,
    }

    public BLOCK[][][] grid;

    public GridLayout(int sizeX, int sizeY, int sizeZ) {
        grid = new BLOCK[sizeX][][];
        for (int i = 0; i < grid.length; i++) {
            grid[i] = new BLOCK[sizeY][];
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = new BLOCK[sizeZ];
                for (int k = 0; k < grid[i][j].length; k++) {
                    grid[i][j][k] = BLOCK.AIR;
                }
            }
        }
    }

    public BLOCK get(Vec3i v) {
        return grid[v.getX()][v.getY()][v.getZ()];
    }

    public void set(Vec3i v, BLOCK block) {
        grid[v.getX()][v.getY()][v.getZ()] = block;
    }
}
