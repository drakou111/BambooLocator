package dev.drakou111;

public enum BlockType {
    BAMBOO0(0.25F),
    DRIPSTONE(0.125F);

    public final float maxOffset;

    BlockType(float maxOffset) {
        this.maxOffset = maxOffset;
    }
}
