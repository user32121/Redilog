package redilog.routing;

public class GridLayout {
    public static class Connections {
        public boolean posX, negX, posY, negY, posZ, negZ;
    }

    public Connections[][][] grid;

    public GridLayout(int sizeX, int sizeY, int sizeZ) {
        grid = new Connections[sizeX][][];
        for (int i = 0; i < grid.length; i++) {
            grid[i] = new Connections[sizeY][];
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = new Connections[sizeZ];
            }
        }
    }
}
