package redilog.utils;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vec3i;

public class VecUtil {
    public static Vec3f i2f(Vec3i v) {
        return new Vec3f(v.getX(), v.getY(), v.getZ());
    }

    public static Vec3i f2i(Vec3f v) {
        return new Vec3i(v.getX(), v.getY(), v.getZ());
    }

    public static Vec3d i2d(Vec3i v) {
        return new Vec3d(v.getX(), v.getY(), v.getZ());
    }

    public static Vec3i d2i(Vec3d v) {
        return new Vec3i(v.getX(), v.getY(), v.getZ());
    }

    public static Vec3d avg(Vec3d... vs) {
        if (vs.length == 0) {
            return Vec3d.ZERO;
        }
        Vec3d ret = Vec3d.ZERO;
        for (Vec3d v : vs) {
            ret = ret.add(v);
        }
        return ret.multiply(1.0 / vs.length);
    }
}
