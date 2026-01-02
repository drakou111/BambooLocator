package dev.drakou111.bruteforce;

import dev.drakou111.ProgressListener;

public interface Bruteforcer {
    void run();
    void stop();
    void setProgressListener(ProgressListener listener);
}