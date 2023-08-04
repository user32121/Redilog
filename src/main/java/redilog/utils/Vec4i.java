package redilog.utils;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public class Vec4i {
    private int x;
    private int y;
    private int z;
    private int w;

    public Vec4i(Vec3i v, int w) {
        this(v.getX(), v.getY(), v.getZ(), w);
    }

    public Vec4i(int x, int y, int z, int w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vec3i to3i() {
        return new Vec3i(x, y, z);
    }

    public Vec4i add(int dx, int dy, int dz, int dw) {
        return new Vec4i(x + dx, y + dy, z + dz, w + dw);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getW() {
        return w;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public void setW(int w) {
        this.w = w;
    }

    public Vec4i offset(Direction direction) {
        return new Vec4i(to3i().offset(direction), w);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vec4i other = (Vec4i) obj;
        if (x != other.x || y != other.y || z != other.z || w != other.w)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("Vec4i [x=%d, y=%d, z=%d, w=%d]", x, y, z, w);
    }

}
