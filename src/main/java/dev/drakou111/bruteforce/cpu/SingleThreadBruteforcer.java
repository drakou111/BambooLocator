package dev.drakou111.bruteforce.cpu;

import dev.drakou111.Kernel;
import dev.drakou111.ProgressListener;
import dev.drakou111.bruteforce.Bruteforcer;
import dev.drakou111.ui.Logger;

public class SingleThreadBruteforcer implements Bruteforcer {

    private ProgressListener progressListener = null;

    private final Kernel kernel;
    private final int minX, maxX, minZ, maxZ;
    private final Logger logger;
    private volatile boolean running = true;

    public SingleThreadBruteforcer(Kernel kernel, int minX, int maxX, int minZ, int maxZ, Logger logger) {
        this.kernel = kernel;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.logger = logger;
    }

    @Override
    public void run() {
        running = true;
        long start = System.currentTimeMillis();
        logger.log("Single-thread bruteforce starting...");

        long width = (long)maxX - minX + 1L;
        long height = (long)maxZ - minZ + 1L;
        final long total = width * height;
        long processed = 0L;
        double lastReported = -1.0;

        for (int z = minZ; z <= maxZ && running; z++) {
            for (int x = minX; x <= maxX && running; x++) {
                if (kernel.checkKernelAt(x, z)) {
                    logger.log("/tp @s " + x + " ~ " + z);
                }

                processed++;
                if (processed % 1000 == 0 && progressListener != null) {
                    double frac = (double) processed / (double) total;
                    if (frac - lastReported >= 0.005 || processed == total) {
                        lastReported = frac;
                        progressListener.onProgress(Math.min(1.0, frac));
                    }
                }
            }
        }
        if (progressListener != null) progressListener.onProgress(1.0);

        long elapsed = System.currentTimeMillis() - start;
        logger.log("Single-thread done. (took " + elapsed + " ms)");
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }
}
