package redilog.utils;

public class Array4D<T> {

    @FunctionalInterface
    public static interface Supplier4<T> {
        public T get(int x, int y, int z, int w);
    }

    public static class Builder<T> {
        public int sizeX = 0;
        public int sizeY = 0;
        public int sizeZ = 0;
        public int sizeW = 0;
        public Supplier4<T> supplier = (x, y, z, w) -> null;

        public Builder<T> fill(T value) {
            this.supplier = (x, y, z, w) -> value;
            return this;
        }

        public Builder<T> size(int x, int y, int z, int w) {
            sizeX = x;
            sizeY = y;
            sizeZ = z;
            sizeW = w;
            return this;
        }

        public Builder<T> size(Vec4i v) {
            sizeX = v.getX();
            sizeY = v.getY();
            sizeZ = v.getZ();
            sizeW = v.getW();
            return this;
        }

        public Array4D<T> build() {
            return new Array4D<>(sizeX, sizeY, sizeZ, sizeW, supplier);
        }
    }

    private Object[][][][] data;

    /**
     * Make a shallow copy.
     */
    protected Array4D(Array4D<T> other) {
        this.data = other.data;
    }

    protected Array4D(int sizeX, int sizeY, int sizeZ, int sizeW, Supplier4<T> supplier) {
        data = new Object[sizeX][][][];
        for (int x = 0; x < sizeX; ++x) {
            data[x] = new Object[sizeY][][];
            for (int y = 0; y < sizeY; ++y) {
                data[x][y] = new Object[sizeZ][];
                for (int z = 0; z < sizeZ; ++z) {
                    data[x][y][z] = new Object[sizeW];
                    for (int w = 0; w < sizeW; ++w) {
                        data[x][y][z][w] = supplier.get(x, y, z, w);
                    }
                }
            }
        }
    }

    public T get(Vec4i v) {
        return get(v.getX(), v.getY(), v.getZ(), v.getW());
    }

    @SuppressWarnings("unchecked")
    public T get(int x, int y, int z, int w) {
        return (T) data[x][y][z][w];
    }

    public void set(Vec4i v, T value) {
        set(v.getX(), v.getY(), v.getZ(), v.getW(), value);
    }

    public void set(int x, int y, int z, int w, T value) {
        data[x][y][z][w] = value;
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

    public int getWLength() {
        //avoid exceptions on degenerate cases
        return data.length > 0 && data[0].length > 0 && data[0][0].length > 0 ? data[0][0][0].length : 0;
    }

    public Vec4i getSize() {
        return new Vec4i(getXLength(), getYLength(), getZLength(), getWLength());
    }

    public boolean inBounds(Vec4i v) {
        return inBounds(v.getX(), v.getY(), v.getZ());
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < getXLength() && y < getYLength() && z < getZLength();
    }

    public boolean isValue(Vec4i v, T value) {
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
