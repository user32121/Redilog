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
}
