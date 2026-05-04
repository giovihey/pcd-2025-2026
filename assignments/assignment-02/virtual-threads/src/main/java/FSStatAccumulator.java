import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public final class FSStatAccumulator {

    private final AtomicLong totalFiles;
    private final AtomicLongArray bandCounts;
    private final long maxFileSize;
    private final int  numBands;
    private final long bandSize;

    public FSStatAccumulator(long maxFileSize, int numBands) {
        if (numBands <= 0) throw new IllegalArgumentException("numBands must be > 0");
        if (maxFileSize <= 0) throw new IllegalArgumentException("maxFileSize must be > 0");

        this.maxFileSize = maxFileSize;
        this.numBands    = numBands;
        this.bandSize    = maxFileSize / numBands;
        this.totalFiles  = new AtomicLong(0);
        // NB regular bands + 1 overflow band
        this.bandCounts  = new AtomicLongArray(numBands + 1);
    }

    /**
     * Records a single file with the given byte size.
     * This method is safe to call from any virtual thread concurrently.
     */
    public void recordFile(long fileSizeBytes) {
        totalFiles.incrementAndGet();

        int bandIndex;
        if (fileSizeBytes >= maxFileSize) {
            bandIndex = numBands;
        } else {
            bandIndex = (int) (fileSizeBytes / bandSize);
            // clamp – edge case when fileSizeBytes == maxFileSize - 1 and numBands is large
            if (bandIndex >= numBands) bandIndex = numBands - 1;
        }
        bandCounts.incrementAndGet(bandIndex);
    }

    /**
     * Builds and returns an immutable {@link FSReport} snapshot.
     * Typically called once, after all virtual threads have finished.
     */
    public FSReport buildReport() {
        long total = totalFiles.get();
        long[] bands = new long[numBands + 1];
        for (int i = 0; i < bands.length; i++) {
            bands[i] = bandCounts.get(i);
        }
        return new FSReport(total, bands, maxFileSize, numBands);
    }
}