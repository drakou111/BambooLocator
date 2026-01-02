package dev.drakou111.bruteforce.gpu;

import dev.drakou111.Bounds;
import dev.drakou111.Kernel;
import dev.drakou111.ProgressListener;
import dev.drakou111.bruteforce.Bruteforcer;
import dev.drakou111.ui.Logger;
import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static jcuda.driver.JCudaDriver.*;

public class JCudaBruteforcer implements Bruteforcer {

	private static final long MAX_POSITIONS_PER_LAUNCH = 4_194_304L;
	public static final String CUDA_FILE = "/kern.ptx";

	private final int tileW;
	private final int tileH;
	private final int threads;
	private final int blocks;

	private final Kernel kernel;
	private final long minX, maxX, minZ, maxZ;
	private final Logger logger;

	private volatile boolean running = true;
	private ProgressListener progressListener = null;

	public JCudaBruteforcer(
			Kernel kernel,
			long minX, long maxX,
			long minZ, long maxZ,
			Logger logger,
			int tileW,
			int tileH,
			int threads,
			int blocks
						   ) {
		this.kernel = Objects.requireNonNull(kernel);
		this.minX = minX;
		this.maxX = maxX;
		this.minZ = minZ;
		this.maxZ = maxZ;
		this.logger = Objects.requireNonNull(logger);

		this.tileW = Math.max(1, tileW);
		this.tileH = Math.max(1, tileH);

		int t = Math.max(32, threads);
		t = (t / 32) * 32;
		if (t == 0) t = 32;
		this.threads = t;

		this.blocks = Math.max(1, blocks);
	}

	@Override
	public void setProgressListener(ProgressListener listener) {
		this.progressListener = listener;
	}

	@Override
	public void run() {
		running = true;
		long start = System.currentTimeMillis();
		logger.log("CUDA bruteforce starting...");

		CUcontext context = null;
		CUdeviceptr dMinXs = null, dMaxXs = null, dMinZs = null, dMaxZs = null;
		CUdeviceptr dHitCount = null, dHitXs = null, dHitZs = null;

		try {
			JCudaDriver.setExceptionsEnabled(true);
			cuInit(0);

			CUdevice device = new CUdevice();
			cuDeviceGet(device, 0);

			context = new CUcontext();
			cuCtxCreate(context, 0, device);

			String ptxPath = extractResourceToTemp(CUDA_FILE);

			CUmodule module = new CUmodule();
			cuModuleLoad(module, ptxPath);

			CUfunction function = new CUfunction();
			cuModuleGetFunction(function, module, "checkKernelMultiTileBatch");

			int kw = kernel.grid[0].length;
			int kh = kernel.grid.length;
			int kSize = kw * kh;

			float[] minXs = new float[kSize];
			float[] maxXs = new float[kSize];
			float[] minZs = new float[kSize];
			float[] maxZs = new float[kSize];

			for (int z = 0; z < kh; z++) {
				for (int x = 0; x < kw; x++) {
					int i = z * kw + x;
					Bounds b = kernel.grid[z][x];
					minXs[i] = b.minX;
					maxXs[i] = b.maxX;
					minZs[i] = b.minZ;
					maxZs[i] = b.maxZ;
				}
			}

			dMinXs = upload(minXs);
			dMaxXs = upload(maxXs);
			dMinZs = upload(minZs);
			dMaxZs = upload(maxZs);

			dHitCount = new CUdeviceptr();
			cuMemAlloc(dHitCount, Sizeof.INT);

			long maxHitsPerBatch = MAX_POSITIONS_PER_LAUNCH;
			dHitXs = new CUdeviceptr();
			dHitZs = new CUdeviceptr();
			cuMemAlloc(dHitXs, maxHitsPerBatch * Sizeof.LONG);
			cuMemAlloc(dHitZs, maxHitsPerBatch * Sizeof.LONG);

			int tilesX = (int) ((maxX - minX + 1 + tileW - 1) / tileW);
			int tilesZ = (int) ((maxZ - minZ + 1 + tileH - 1) / tileH);
			long totalTiles = (long) tilesX * tilesZ;
			long tileSize = (long) tileW * tileH;
			long totalPositions = totalTiles * tileSize;

			int tilesPerBatch = (int) Math.max(1, Math.min(totalTiles, MAX_POSITIONS_PER_LAUNCH / Math.max(1, tileSize)));

			long processed = 0;
			long tileStart = 0;

			while (tileStart < totalTiles && running) {

				int batchTiles = (int) Math.min(tilesPerBatch, totalTiles - tileStart);
				cuMemsetD32(dHitCount, 0, 1);

				Pointer args = Pointer.to(
						Pointer.to(new long[]{minX}),
						Pointer.to(new long[]{minZ}),
						Pointer.to(new int[]{tilesX}),
						Pointer.to(new int[]{tilesZ}),
						Pointer.to(new int[]{tileW}),
						Pointer.to(new int[]{tileH}),
						Pointer.to(new int[]{kw}),
						Pointer.to(new int[]{kh}),
						Pointer.to(dMinXs),
						Pointer.to(dMaxXs),
						Pointer.to(dMinZs),
						Pointer.to(dMaxZs),
						Pointer.to(new float[]{kernel.blockType.maxOffset}),
						Pointer.to(dHitCount),
						Pointer.to(dHitXs),
						Pointer.to(dHitZs),
						Pointer.to(new long[]{tileStart}),
						Pointer.to(new int[]{batchTiles})
				);

				cuLaunchKernel(function, blocks, 1, 1, threads, 1, 1, 0, null, args, null);

				cuCtxSynchronize();

				int[] hitCountArr = new int[1];
				cuMemcpyDtoH(Pointer.to(hitCountArr), dHitCount, Sizeof.INT);
				int found = hitCountArr[0];

				if (found > 0) {
					long[] xs = new long[found];
					long[] zs = new long[found];
					cuMemcpyDtoH(Pointer.to(xs), dHitXs, (long) found * Sizeof.LONG);
					cuMemcpyDtoH(Pointer.to(zs), dHitZs, (long) found * Sizeof.LONG);

					for (int i = 0; i < found && running; i++) {
						logger.log("/tp @s " + xs[i] + " ~ " + zs[i]);
					}
				}

				processed += (long) batchTiles * tileSize;
				if (progressListener != null) {
					double frac = Math.min(1.0, (double) processed / (double) totalPositions);
					progressListener.onProgress(frac);
				}

				tileStart += batchTiles;
			}

			if (progressListener != null) progressListener.onProgress(1.0);

			logger.log("JCuda done in " + (System.currentTimeMillis() - start) + " ms");

		} catch (Throwable t) {
			logger.log("JCuda error: " + t.getMessage());
			t.printStackTrace();
		} finally {
			try {
				if (dMinXs != null) cuMemFree(dMinXs);
				if (dMaxXs != null) cuMemFree(dMaxXs);
				if (dMinZs != null) cuMemFree(dMinZs);
				if (dMaxZs != null) cuMemFree(dMaxZs);
				if (dHitCount != null) cuMemFree(dHitCount);
				if (dHitXs != null) cuMemFree(dHitXs);
				if (dHitZs != null) cuMemFree(dHitZs);
			} catch (Throwable ignored) {}

			try {
				if (context != null) cuCtxDestroy(context);
			} catch (Throwable ignored) {}
		}
	}

	private CUdeviceptr upload(float[] data) {
		CUdeviceptr ptr = new CUdeviceptr();
		cuMemAlloc(ptr, (long) data.length * Sizeof.FLOAT);
		cuMemcpyHtoD(ptr, Pointer.to(data), (long) data.length * Sizeof.FLOAT);
		return ptr;
	}

	private static String extractResourceToTemp(String resourcePath) {
		try (InputStream is = JCudaBruteforcer.class.getResourceAsStream(resourcePath)) {
			if (is == null) throw new RuntimeException("Resource not found: " + resourcePath);
			Path temp = Files.createTempFile("jcuda_kern_", ".ptx");
			Files.copy(is, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			temp.toFile().deleteOnExit();
			return temp.toAbsolutePath().toString();
		} catch (Exception e) {
			throw new RuntimeException("Failed to extract CUDA resource", e);
		}
	}

	@Override
	public void stop() {
		running = false;
	}
}
