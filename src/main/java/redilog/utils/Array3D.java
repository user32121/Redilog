package redilog.utils;

import java.util.function.Supplier;

import net.minecraft.util.math.Vec3i;

public class Array3D<T> {

    public T[][][] data;

    public Array3D(Vec3i size) {
        this(size.getX(), size.getY(), size.getZ());
    }

    public Array3D(int sizeX, int sizeY, int sizeZ) {
        this(sizeX, sizeY, sizeZ, new ConstantSupplier<>(null));
    }

    public Array3D(int sizeX, int sizeY, int sizeZ, T value) {
        this(sizeX, sizeY, sizeZ, new ConstantSupplier<>(value));
    }

    @SuppressWarnings("unchecked")
    public Array3D(int sizeX, int sizeY, int sizeZ, Supplier<T> supplier) {
        data = (T[][][]) new Object[sizeX][][];
        for (int i = 0; i < data.length; i++) {
            data[i] = (T[][]) new Object[sizeY][];
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = (T[]) new Object[sizeZ];
                for (int k = 0; k < data[i][j].length; k++) {
                    data[i][j][k] = supplier.get();
                }
            }
        }
    }

    public T get(Vec3i v) {
        return get(v.getX(), v.getY(), v.getZ());
    }

    public T get(int x, int y, int z) {
        return data[x][y][z];
    }

    public void set(Vec3i v, T value) {
        set(v.getX(), v.getY(), v.getZ(), value);
    }

    public void set(int x, int y, int z, T value) {
        data[x][y][z] = value;
    }

    public Vec3i getSize() {
        return new Vec3i(data.length,
                //avoid exceptions on degenerate cases
                data.length > 0 ? data[0].length : 0,
                data.length > 0 && data[0].length > 0 ? data[0][0].length : 0);
    }
}
