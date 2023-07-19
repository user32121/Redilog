package redilog.utils;

import java.util.function.Supplier;

import net.minecraft.util.math.Vec3i;

public class Array3D<T> {

    private Object[][][] data;

    /**
     * Make a shallow copy.
     */
    protected Array3D(Array3D<T> other) {
        this.data = other.data;
    }

    public Array3D(Vec3i size) {
        this(size.getX(), size.getY(), size.getZ());
    }

    public Array3D(int sizeX, int sizeY, int sizeZ) {
        this(sizeX, sizeY, sizeZ, new ConstantSupplier<>(null));
    }

    public Array3D(int sizeX, int sizeY, int sizeZ, T value) {
        this(sizeX, sizeY, sizeZ, new ConstantSupplier<>(value));
    }

    public Array3D(int sizeX, int sizeY, int sizeZ, Supplier<T> supplier) {
        data = new Object[sizeX][][];
        for (int i = 0; i < data.length; i++) {
            data[i] = new Object[sizeY][];
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = new Object[sizeZ];
                for (int k = 0; k < data[i][j].length; k++) {
                    data[i][j][k] = supplier.get();
                }
            }
        }
    }

    public T get(Vec3i v) {
        return get(v.getX(), v.getY(), v.getZ());
    }

    @SuppressWarnings("unchecked")
    public T get(int x, int y, int z) {
        return (T) data[x][y][z];
    }

    public void set(Vec3i v, T value) {
        set(v.getX(), v.getY(), v.getZ(), value);
    }

    public void set(int x, int y, int z, T value) {
        data[x][y][z] = value;
    }

    public int getXLength() {
        return data.length;
    }

    public int getYLength() {
        //avoid exceptions on degenerate cases
        return data.length > 0 ? data[0].length : 0;
    }

    public int getZLength() {
        //avoid exceptions on degenerate cases
        return data.length > 0 && data[0].length > 0 ? data[0][0].length : 0;
    }

    public Vec3i getSize() {
        return new Vec3i(getXLength(), getYLength(), getZLength());
    }

    public boolean inBounds(Vec3i v) {
        return inBounds(v.getX(), v.getY(), v.getZ());
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < getXLength() && y < getYLength() && z < getZLength();
    }

    public boolean isValue(Vec3i v, T value) {
        if (!inBounds(v)) {
            return false;
        }
        if (value == null) {
            return get(v) == null;
        } else {
            return value.equals(get(v));
        }
    }
}
