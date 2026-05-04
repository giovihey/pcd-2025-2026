import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FSStatLib {

    /**
     * Computes a filesystem statistics report for directory {@code dir}.
     *
     * <p>The call returns a {@link CompletableFuture} immediately; the
     * actual computation runs on virtual threads in the background.
     *
     * @param dir         root directory to scan (recursively)
     * @param maxFileSize upper boundary for the size bands (bytes)
     * @param numBands    number of equally-sized bands in [0, maxFileSize]
     * @return            a future that completes with the {@link FSReport}
     */
    public CompletableFuture<FSReport> getFSReport(Path dir, long maxFileSize, int numBands) {
        FSStatAccumulator accumulator = new FSStatAccumulator(maxFileSize, numBands);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        // Start recursive traversal; chain report building on full completion.
        return traverseDirectory(dir, accumulator, executor)
                .whenComplete((v, err) -> executor.shutdown())
                .thenApply(v -> accumulator.buildReport());
    }

    /**
     * Asynchronously scans {@code dir} on a virtual thread.
     *
     * <p>Returns a future that completes only when this directory AND
     * all of its subdirectories (recursively) have been fully scanned.
     * This is achieved by collecting the child futures and combining
     * them with {@link CompletableFuture#allOf}.
     *
     * <p>The "sequential inside, parallel across directories" structure
     * is the idiomatic Virtual Thread pattern: each virtual thread runs
     * blocking I/O sequentially (easy to read), while many such threads
     * run concurrently across the directory tree (high throughput).
     */
    private CompletableFuture<Void> traverseDirectory(Path dir,
                                                      FSStatAccumulator accumulator,
                                                      ExecutorService executor) {
        return CompletableFuture.runAsync(() -> {
            List<CompletableFuture<Void>> childFutures = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    BasicFileAttributes attrs;
                    try {
                        attrs = Files.readAttributes(entry, BasicFileAttributes.class,
                                LinkOption.NOFOLLOW_LINKS);
                    } catch (IOException e) {
                        // Skip entries we cannot stat (permissions, broken symlinks)
                        continue;
                    }

                    if (attrs.isRegularFile()) {
                        // Counted synchronously - no need for a separate VT per file
                        accumulator.recordFile(attrs.size());
                    } else if (attrs.isDirectory()) {
                        // Spawn a new virtual thread for each subdirectory
                        childFutures.add(traverseDirectory(entry, accumulator, executor));
                    }
                }
            } catch (IOException e) {
                System.err.println("[VT] Cannot access: " + dir + " - " + e.getMessage());
            }

            // Block this virtual thread until ALL child directories are done.
            // Blocking here is cheap - the VT yields its carrier thread to others.
            if (!childFutures.isEmpty()) {
                CompletableFuture.allOf(childFutures.toArray(new CompletableFuture[0])).join();
            }

        }, executor);
    }
}