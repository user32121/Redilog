package redilog.utils;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec3i;

public class NbtUtil {
    public static Vec3i readv3i(NbtElement nbt) {
        int[] ar = null;
        if (nbt instanceof NbtIntArray nbtia) {
            ar = nbtia.getIntArray();
        }
        if (nbt instanceof NbtList nbtl) {
            ar = new int[nbtl.size()];
            for (int i = 0; i < nbtl.size(); ++i) {
                ar[i] = nbtl.getInt(i);
            }
        }
        if (ar == null) {
            throw new AssertionError(String.format("%s is not an NbtIntArray nor an NbtList of NbtInt", nbt));
        }
        if (ar.length != 3) {
            throw new AssertionError(String.format("%s has %s elements (expected 3)", nbt, ar.length));
        }
        return new Vec3i(ar[0], ar[1], ar[2]);
    }

    public static Vec4i readv4i(NbtElement nbt) {
        int[] ar = null;
        if (nbt instanceof NbtIntArray nbtia) {
            ar = nbtia.getIntArray();
        }
        if (nbt instanceof NbtList nbtl) {
            ar = new int[nbtl.size()];
            for (int i = 0; i < nbtl.size(); ++i) {
                ar[i] = nbtl.getInt(i);
            }
        }
        if (ar == null) {
            throw new AssertionError(String.format("%s is not an NbtIntArray nor an NbtList of NbtInt", nbt));
        }
        if (ar.length != 4) {
            throw new AssertionError(String.format("%s has %s elements (expected 4)", nbt, ar.length));
        }
        return new Vec4i(ar[0], ar[1], ar[2], ar[3]);
    }
}
