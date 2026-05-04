import java.util.Arrays;

/**
 * Immutable value object holding the result of a filesystem statistics report.
 *
 * <p>Contains:
 * <ul>
 *   <li>The total number of files found under the scanned directory (recursively).</li>
 *   <li>The distribution of file sizes across {@code NB} equally-sized bands in [0, MaxFS],
 *       plus one extra band for files larger than MaxFS.</li>
 * </ul>
 */
public final class FSReport {
    private final long totalFiles;
    private final long[] bandCounts;
    private final long maxFileSize;
    private final int numBands;

    public FSReport(long totalFiles, long[] bandCounts, long maxFileSize, int numBands) {
        this.totalFiles  = totalFiles;
        this.bandCounts  = Arrays.copyOf(bandCounts, bandCounts.length);
        this.maxFileSize = maxFileSize;
        this.numBands    = numBands;
    }

    public long getTotalFiles() { return totalFiles; }
    public long getBandCount(int index) { return bandCounts[index]; }
    public int getTotalBands() { return bandCounts.length; }
    public long getMaxFileSize() { return maxFileSize; }
    public int getNumBands() { return numBands; }

    /**
     * Returns a range label for the given band index.
     * e.g. "[0, 1024)" or "(4096, ∞)"
     */
    public String getBandLabel(int index) {
        long bandSize = maxFileSize / numBands;
        if (index < numBands) {
            long lo = index * bandSize;
            long hi = (index == numBands - 1) ? maxFileSize : lo + bandSize;
            return String.format("[%d, %d) bytes", lo, hi);
        } else {
            return String.format("(%d, +inf) bytes  [overflow]", maxFileSize);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== FSReport ===\n");
        sb.append(String.format("Total files : %d%n", totalFiles));
        sb.append(String.format("Bands (MaxFS=%d, NB=%d):%n", maxFileSize, numBands));
        for (int i = 0; i < bandCounts.length; i++) {
            sb.append(String.format("  Band %2d  %-30s  -> %d files%n",
                    i, getBandLabel(i), bandCounts[i]));
        }
        return sb.toString();
    }
}