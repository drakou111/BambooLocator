package dev.drakou111.utils;

import dev.drakou111.Bounds;

public final class KernelEstimator {

    public static Result estimate(Bounds[][] kernel, float[] OFFSETS, long x0, long x1, long z0, long z1) {
        int kernelBlocks = kernel.length * kernel[0].length;

        double logPsingle = 0.0;
        int minAllowed = Integer.MAX_VALUE;

        for (Bounds[] row : kernel) {
            for (Bounds b : row) {
                int allowed = countAllowed(b, OFFSETS);
                minAllowed = Math.min(minAllowed, allowed);

                if (allowed == 0) {
                    return new Result(0, kernelBlocks, 0, 0, 0, 0);
                }

                logPsingle += Math.log((double) allowed / (OFFSETS.length * OFFSETS.length));
            }
        }

        double pSingle = Math.exp(logPsingle);

        long width  = x1 - x0 + 1;
        long height = z1 - z0 + 1;
        double attempts = (double) width * height;

        double pAtLeastOne =
            pSingle < 1e-6
            ? 1.0 - Math.exp(-attempts * pSingle)
            : 1.0 - Math.pow(1.0 - pSingle, attempts);

        long expectedCount = (long)(attempts * pSingle);

        return new Result(
            pAtLeastOne,
            kernelBlocks,
            attempts,
            pSingle,
            expectedCount,
            minAllowed
        );
    }

    private static int countAllowed(Bounds b, float[] OFFSETS) {
        int c = 0;
        for (float ox : OFFSETS) {
            for (float oz : OFFSETS) {
                if (b.isInRange(ox, oz)) {
                    c++;
                }
            }
        }
        return c;
    }

    public record Result(
            double probability,
            int kernelBlocks,
            double attempts,
            double pSingle,
            double expectedCount,
            int minAllowedOffsets
    ) {}
}
