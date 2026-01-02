package dev.drakou111.bruteforce;

public enum BruteforceMode {
    SINGLE_THREAD("Single Thread (CPU)"),
    MULTI_THREAD("Multi Thread (CPU)"),
    CUDA("JCuda (GPU)");

    public final String label;

    BruteforceMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}