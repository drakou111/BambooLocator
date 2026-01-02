package dev.drakou111;

public class Bounds {
    public float minX;
    public float maxX;
    public float minZ;
    public float maxZ;

    private static final float EPSILON = 0.00001F;

    public Bounds(float minX, float maxX, float minZ, float maxZ) {
        this.minX = minX - EPSILON;
        this.maxX = maxX + EPSILON;
        this.minZ = minZ - EPSILON;
        this.maxZ = maxZ + EPSILON;
    }

    public Bounds(float x, float z) {
        this.minX = x - EPSILON;
        this.maxX = x + EPSILON;
        this.minZ = z - EPSILON;
        this.maxZ = z + EPSILON;
    }

    public Bounds() {
        this.minX = -1;
        this.maxX = 1;
        this.minZ = -1;
        this.maxZ = 1;
    }

    public boolean isInRange(float x, float z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}
