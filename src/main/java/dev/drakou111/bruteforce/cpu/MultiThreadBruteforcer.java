package dev.drakou111.bruteforce.cpu;

import dev.drakou111.Kernel;
import dev.drakou111.ProgressListener;
import dev.drakou111.bruteforce.Bruteforcer;
import dev.drakou111.ui.Logger;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MultiThreadBruteforcer implements Bruteforcer {

    private ProgressListener progressListener = null;
    private final Kernel kernel;
    private final int minX, maxX, minZ, maxZ;
    private final Logger logger;
    private final int threadCount;

    private volatile boolean running = true;

    public MultiThreadBruteforcer(Kernel kernel, int minX, int maxX, int minZ, int maxZ, Logger logger) {
        this(kernel, minX, maxX, minZ, maxZ, logger, Runtime.getRuntime().availableProcessors() - 1);
    }

    public MultiThreadBruteforcer(Kernel kernel, int minX, int maxX, int minZ, int maxZ, Logger logger, int threadCount) {
        this.kernel = Objects.requireNonNull(kernel);
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.logger = Objects.requireNonNull(logger);
        this.threadCount = Math.max(1, threadCount);
    }

    @Override
    public void run() {
        running = true;
        long start = System.currentTimeMillis();
        logger.log("Multi-thread bruteforce starting with " + threadCount + " threads...");

        AtomicLong processed = new AtomicLong(0L);

        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor();

        long total = (maxX - minX + 1L) * (maxZ - minZ + 1L);
        if (progressListener != null) {
            reporter.scheduleAtFixedRate(() -> {
                double frac = Math.min(1.0, (double) processed.get() / (double) total);
                progressListener.onProgress(frac);
            }, 0, 100, TimeUnit.MILLISECONDS);
        }

        int totalZ = maxZ - minZ + 1;
        int chunk = Math.max(1, totalZ / threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int zStart = minZ + t * chunk;
            final int zEnd = (t == threadCount - 1) ? maxZ : Math.min(maxZ, zStart + chunk - 1);

            exec.submit(() -> {
                try {
                    long localProcessed = 0;
                    for (int z = zStart; z <= zEnd && running; z++) {
                        for (int x = minX; x <= maxX && running; x++) {
                            if (kernel.checkKernelAt(x, z)) {
                                logger.log("/tp @s " + x + " ~ " + z);
                            }

                            localProcessed++;
                            if (localProcessed >= 256) {
                                processed.addAndGet(localProcessed);
                                localProcessed = 0;
                            }
                        }
                    }
                    if (localProcessed > 0) {
                        processed.addAndGet(localProcessed);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            // ignore
        } finally {
            exec.shutdownNow();
            reporter.shutdownNow();
        }

        if (progressListener != null) progressListener.onProgress(1.0);

        long elapsed = System.currentTimeMillis() - start;
        logger.log("Multi-thread done. (took " + elapsed + " ms)");
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
