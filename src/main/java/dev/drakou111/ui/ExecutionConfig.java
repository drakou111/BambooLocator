package dev.drakou111.ui;

import dev.drakou111.bruteforce.BruteforceMode;

public class ExecutionConfig {

    public BruteforceMode mode;

    public int tileW;
    public int tileH;
    public int cudaThreads;
    public int cudaBlocks;

    public int cpuThreads;

    public static ExecutionConfig defaultConfig(int smCount) {
        ExecutionConfig c = new ExecutionConfig();

        int cores = Runtime.getRuntime().availableProcessors();

        if (smCount > 0) {
            c.mode = BruteforceMode.CUDA;
            c.tileW = 64;
            c.tileH = 64;
            c.cudaThreads = 256;
            c.cudaBlocks = smCount * 32;
        } else {
            c.cpuThreads = Math.max(1, cores - 1);
            c.mode = (c.cpuThreads <= 4)
                     ? BruteforceMode.SINGLE_THREAD
                     : BruteforceMode.MULTI_THREAD;
        }

        return c;
    }
}

