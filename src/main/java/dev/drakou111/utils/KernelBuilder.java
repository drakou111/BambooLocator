package dev.drakou111.utils;

import dev.drakou111.BlockType;
import dev.drakou111.Bounds;
import dev.drakou111.Kernel;
import dev.drakou111.ui.KernelCell;

public final class KernelBuilder {

    private static final double FULL_EDGE = 0.25;

    private KernelBuilder() {
    }

    public static Kernel fromCells(KernelCell[][] cells, int gridSize) {
        int minCellX = gridSize;
        int minCellY = gridSize;
        int maxCellX = -1;
        int maxCellY = -1;

        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                Bounds r = cells[y][x].getRegion();
                boolean isFull = r.minX <= -FULL_EDGE && r.maxX >= FULL_EDGE && r.minZ <= -FULL_EDGE && r.maxZ >= FULL_EDGE;

                if (!isFull) {
                    minCellX = Math.min(minCellX, x);
                    minCellY = Math.min(minCellY, y);
                    maxCellX = Math.max(maxCellX, x);
                    maxCellY = Math.max(maxCellY, y);
                }
            }
        }

        if (maxCellX == -1) {
            return null;
        }

        int width = maxCellX - minCellX + 1;
        int height = maxCellY - minCellY + 1;

        Bounds[][] kernelGrid = new Bounds[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                kernelGrid[y][x] = cells[minCellY + y][minCellX + x].toBounds();
            }
        }

        return new Kernel(kernelGrid, BlockType.BAMBOO0);
    }
}
