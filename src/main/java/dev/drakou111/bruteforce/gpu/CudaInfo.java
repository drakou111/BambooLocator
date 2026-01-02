package dev.drakou111.bruteforce.gpu;

import jcuda.driver.CUdevice;
import jcuda.driver.CUdevice_attribute;
import jcuda.driver.JCudaDriver;

import static jcuda.driver.JCudaDriver.*;

public final class CudaInfo {

    static {
        try {
            JCudaDriver.setExceptionsEnabled(true);
        } catch (Throwable ignored) {}
    }

    public static boolean isCudaAvailable() {
        try {
            cuInit(0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static DeviceInfo queryDevice() {
        try {
            cuInit(0);

            CUdevice device = new CUdevice();
            cuDeviceGet(device, 0);

            int[] smCount = new int[1];
            int[] maxThreadsPerBlock = new int[1];
            int[] warpSize = new int[1];

            cuDeviceGetAttribute(smCount, CUdevice_attribute.CU_DEVICE_ATTRIBUTE_MULTIPROCESSOR_COUNT, device);
            cuDeviceGetAttribute(maxThreadsPerBlock, CUdevice_attribute.CU_DEVICE_ATTRIBUTE_MAX_THREADS_PER_BLOCK, device);
            cuDeviceGetAttribute(warpSize, CUdevice_attribute.CU_DEVICE_ATTRIBUTE_WARP_SIZE, device);

            return new DeviceInfo(smCount[0], maxThreadsPerBlock[0], warpSize[0]);
        } catch (Throwable t) {
            return null;
        }
    }

    public record DeviceInfo(
        int smCount,
        int maxThreadsPerBlock,
        int warpSize
    ) {}
}
