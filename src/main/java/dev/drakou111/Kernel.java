package dev.drakou111;

public class Kernel {
    BlockType blockType;
    Bounds[][] grid;

    public Kernel(Bounds[][] grid, BlockType blockType) {
        this.grid = grid;
        this.blockType = blockType;
    }

    public boolean checkKernelAt(int xOffset, int yOffset) {
        for (int z = 0; z < grid.length; z++) {
            for (int x = 0; x < grid[0].length; x++) {
                Vec2 offset = Utils.getOffsetXZ(x + xOffset, z + yOffset, blockType);
                if (!grid[z][x].isInRange(offset.x(), offset.z()))
                    return false;
            }
        }
        return true;
    }
}
