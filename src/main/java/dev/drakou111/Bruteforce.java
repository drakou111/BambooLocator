package dev.drakou111;

import dev.drakou111.ui.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Bruteforce {
    private static final int MAX_RESULTS = 100;
    private final AtomicInteger resultCount = new AtomicInteger(0);

    public Kernel kernel;
    public int minX;
    public int maxX;
    public int minZ;
    public int maxZ;

    private volatile boolean running = true;
    private final Logger logger;

    public Bruteforce(Kernel kernel, int minX, int maxX, int minZ, int maxZ, Logger logger) {
        this.kernel = kernel;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.logger = logger;
    }

    public void stop() {
        running = false;
    }

    public void run() {
        long startTime = System.currentTimeMillis();
        int threads = Runtime.getRuntime().availableProcessors() - 1;
        logger.log("Starting with " + threads + " threads.");

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int z = minZ; z <= maxZ && running; z++) {
            final int currentZ = z;

            executor.submit(() -> {
                for (int x = minX; x <= maxX && running; x++) {

                    if (resultCount.get() >= MAX_RESULTS) {
                        running = false;
                        break;
                    }

                    if (kernel.checkKernelAt(x, currentZ)) {
                        int count = resultCount.incrementAndGet();

                        if (count <= MAX_RESULTS) {
                            logger.log("/tp @s " + x + " ~ " + currentZ);
                        }

                        if (count >= MAX_RESULTS) {
                            running = false;
                            break;
                        }
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
        } catch (InterruptedException ignored) {}

        long endTime = System.currentTimeMillis();

        logger.log("Done. (took " + (endTime - startTime) + " ms)");
    }

}
