package redilog.utils;

import net.minecraft.util.math.Vec3i;

public class Array3D<T> {

    @FunctionalInterface
    public static interface Supplier3<T> {
        public T get(int x, int y, int z);
    }

    public static class Builder<T> {
        public int sizeX = 0;
        public int sizeY = 0;
        public int sizeZ = 0;
        public Supplier3<T> supplier = (x, y, z) -> null;

        public Builder<T> fill(T value) {
            this.supplier = (x, y, z) -> value;
            return this;
        }

        public Builder<T> size(int x, int y, int z) {
            sizeX = x;
            sizeY = y;
            sizeZ = z;
            return this;
        }

        public Builder<T> size(Vec3i v) {
            sizeX = v.getX();
            sizeY = v.getY();
            sizeZ = v.getZ();
            return this;
        }

        public Array3D<T> build() {
            return new Array3D<>(sizeX, sizeY, sizeZ, supplier);
        }
    }

    private Object[][][] data;

    protected Array3D(int sizeX, int sizeY, int sizeZ, Supplier3<T> supplier) {
        data = new Object[sizeX][][];
        for (int x = 0; x < data.length; ++x) {
            data[x] = new Object[sizeY][];
            for (int y = 0; y < data[x].length; ++y) {
                data[x][y] = new Object[sizeZ];
                for (int z = 0; z < data[x][y].length; ++z) {
                    data[x][y][z] = supplier.get(x, y, z);
                }
            }
        }
    }

    /**
     * Make a shallow copy.
     */
    protected Array3D(Array3D<T> other) {
        this.data = other.data;
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
