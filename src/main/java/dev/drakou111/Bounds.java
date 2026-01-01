package dev.drakou111;

public class Bounds {
    public double minX;
    public double maxX;
    public double minZ;
    public double maxZ;

    private static final double EPSILON = 0.00001;

    public Bounds(double minX, double maxX, double minZ, double maxZ) {
        this.minX = minX - EPSILON;
        this.maxX = maxX + EPSILON;
        this.minZ = minZ - EPSILON;
        this.maxZ = maxZ + EPSILON;
    }

    public Bounds(double x, double z) {
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

    public boolean isInRange(double x, double z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}
