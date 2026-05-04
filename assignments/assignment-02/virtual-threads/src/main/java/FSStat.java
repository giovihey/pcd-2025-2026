import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class FSStat {

    private static final long DEFAULT_MAX_FILE_SIZE = 10L * 1024 * 1024;  // 10 MB
    private static final int  DEFAULT_NUM_BANDS     = 10;

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        if (args.length < 1) {
            System.err.println("Usage: FSStat <directory> [maxFileSizeBytes] [numBands]");
            System.exit(1);
        }

        Path dir = Path.of(args[0]);
        long maxFileSize = args.length > 1 ? Long.parseLong(args[1]) : DEFAULT_MAX_FILE_SIZE;
        int numBands = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_NUM_BANDS;

        System.out.println("=== FSStatLib Virtual Threads ===");
        System.out.printf("Scanning : %s%n", dir.toAbsolutePath());
        System.out.printf("MaxFS    : %d bytes%n", maxFileSize);
        System.out.printf("NumBands : %d%n%n", numBands);

        FSStatLib library = new FSStatLib();

        long startMs = System.currentTimeMillis();

        CompletableFuture<FSReport> futureReport = library.getFSReport(dir, maxFileSize, numBands);

        System.out.println("[main] Future obtained - computation running on virtual threads...");

        FSReport report = futureReport.get();

        long elapsedMs = System.currentTimeMillis() - startMs;

        System.out.println(report);
        System.out.printf("Elapsed  : %d ms%n", elapsedMs);
    }
}