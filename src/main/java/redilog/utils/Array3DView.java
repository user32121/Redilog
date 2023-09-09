package redilog.utils;

/**
 * A wrapper for {@code Array3d} that preserves coordinates but prevents reading or writing 
 * outside of specified bounds.
 */
public class Array3DView<T> extends Array3D<T> {

    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    /**
     * Min and max parameters are relative to the wrapped array.
     * Min is inclusive.
     * Max is exclusive.
     * @param array the array to wrap
     */
    public Array3DView(Array3D<T> array, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        super(array);
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    @Override
    public T get(int x, int y, int z) {
        if (!inBounds(x, y, z)) {
            throw new IndexOutOfBoundsException(String.format("Index (%d,%d,%d) out of bounds for [%d,%d,%d, %d,%d,%d)",
                    x, y, z, minX, minY, minZ, maxX, maxY, maxZ));
        }
        return super.get(x, y, z);
    }

    @Override
    public void set(int x, int y, int z, T value) {
        if (!inBounds(x, y, z)) {
            throw new IndexOutOfBoundsException(String.format("Index (%d,%d,%d) out of bounds for [%d,%d,%d, %d,%d,%d)",
                    x, y, z, minX, minY, minZ, maxX, maxY, maxZ));
        }
        super.set(x, y, z, value);
    }

    @Override
    public boolean inBounds(int x, int y, int z) {
        return x >= minX && y >= minY && z >= minZ && x < maxX && y < maxY && z < maxZ;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

}
