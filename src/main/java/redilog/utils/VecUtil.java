package redilog.utils;

import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vec3i;

public class VecUtil {
    public static Vec3f vec3i2f(Vec3i v) {
        return new Vec3f(v.getX(), v.getY(), v.getZ());
    }

    public static Vec3i vec3f2i(Vec3f v) {
        return new Vec3i(v.getX(), v.getY(), v.getZ());
    }
}
