package dev.drakou111;

@FunctionalInterface
public interface ProgressListener {
    void onProgress(double fraction);
}
