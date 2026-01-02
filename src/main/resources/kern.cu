extern "C"
__global__ void checkKernelMultiTileBatch(
    long long minX,
    long long minZ,
    int tilesX,
    int tilesZ,
    int tileW,
    int tileH,
    int kernelW,
    int kernelH,
    const float* minXs,
    const float* maxXs,
    const float* minZs,
    const float* maxZs,
    float maxOffset,
    int* hitCount,
    long long* hitXs,
    long long* hitZs,
    long long tileStart,
    int tilesInBatch
) {
    int globalThread = blockIdx.x * blockDim.x + threadIdx.x;
    int stride = gridDim.x * blockDim.x;

    long long positionsPerTile = (long long)tileW * tileH;
    long long totalPositions   = (long long)tilesInBatch * positionsPerTile;

    for (long long idx = globalThread; idx < totalPositions; idx += stride) {

        long long localTileIndex = idx / positionsPerTile;
        long long localPosIndex  = idx % positionsPerTile;

        long long tileIndex = tileStart + localTileIndex;
        if (tileIndex >= (long long)tilesX * tilesZ) continue;

        int tileX = tileIndex % tilesX;
        int tileZ = tileIndex / tilesX;

        int localX = localPosIndex % tileW;
        int localZ = localPosIndex / tileW;

        long long worldX = minX + (long long)tileX * tileW + localX;
        long long worldZ = minZ + (long long)tileZ * tileH + localZ;

        bool ok = true;

        #pragma unroll
        for (int kz = 0; kz < kernelH && ok; kz++) {
            #pragma unroll
            for (int kx = 0; kx < kernelW; kx++) {

                long long sx = worldX + kx;
                long long sz = worldZ + kz;

                long long i = (sx * 3129871LL) ^ (sz * 116129781LL);
                i = i * i * 42317861LL + i * 11LL;
                i >>= 16;

                float fx = (((i & 15LL) / 15.0f) - 0.5f) * 0.5f;
                float fz = ((((i >> 8) & 15LL) / 15.0f) - 0.5f) * 0.5f;

                fx = fminf(fmaxf(fx, -maxOffset), maxOffset);
                fz = fminf(fmaxf(fz, -maxOffset), maxOffset);

                int kIndex = kz * kernelW + kx;

                if (fx < minXs[kIndex] || fx > maxXs[kIndex] ||
                    fz < minZs[kIndex] || fz > maxZs[kIndex]) {
                    ok = false;
                    break;
                }
            }
        }

        if (ok) {
            int writeIndex = atomicAdd(hitCount, 1);
            hitXs[writeIndex] = worldX;
            hitZs[writeIndex] = worldZ;
        }
    }
}
