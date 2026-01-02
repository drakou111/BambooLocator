package dev.drakou111.utils;

import dev.drakou111.BlockType;

public class Utils {
    public static long getSeed(int x, int z) {
        long i = (x * 3129871L) ^ ((long) z * 116129781L);
        i = i * i * 42317861L + i * 11L;
        return i >>> 16;
    }

    public static double clamp(double v, double min, double max) {
        if (v <= min) return min;
        if (v >= max) return max;
        return v;
    }

    public static Vec2 getOffsetXZ(int x, int z, BlockType blockType) {
        long i = getSeed(x, z);
        float f = blockType.maxOffset;
        double d0 = clamp(((double)((float)(i & 15L) / 15.0F) - 0.5) * 0.5, -f, f);
        double d1 = clamp(((double)((float)(i >> 8 & 15L) / 15.0F) - 0.5) * 0.5, -f, f);
        return new Vec2((float)d0, (float)d1);
    }
}
