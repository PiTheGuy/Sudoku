package pitheguy.sudoku.solver;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import pitheguy.sudoku.gui.Sudoku;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SolverChecker {
    private static volatile boolean shutdownTriggered = false;

    public static void main(String[] args) throws ParseException {
        if (SudokuSolver.DEBUG) {
            System.err.println("Solver is still in debug mode.");
            System.exit(1);
        }
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(createOptions(), args);
        if (commandLine.hasOption("singlePuzzle")) {
            int puzzleNumber = Integer.parseInt(commandLine.getOptionValue("singlePuzzle"));
            checkSinglePuzzle(puzzleNumber);
            return;
        }
        int iterations = Integer.parseInt(commandLine.getOptionValue("iterations", "10000"));
        int progressUpdateInterval = Integer.parseInt(commandLine.getOptionValue("progressUpdateInterval", "50000"));
        boolean showAllPuzzles = commandLine.hasOption("all");
        run(iterations, progressUpdateInterval, showAllPuzzles);
    }

    public static Options createOptions() {
        Options options = new Options();
        options.addOption("singlePuzzle", true, "Check a single puzzle");
        options.addOption("iterations", true, "Number of iterations to run");
        options.addOption("progressUpdateInterval", true, "Progress update interval");
        options.addOption("all", "Show all unsolved puzzles");
        return options;
    }

    private static void checkSinglePuzzle(int puzzleNumber) {
        long startTime = System.currentTimeMillis();
        Sudoku sudoku = new Sudoku(false);
        sudoku.loadPuzzle(puzzleNumber);
        sudoku.solvePuzzle();
        boolean success = sudoku.isSolved();
        long timeTaken = System.currentTimeMillis() - startTime;
        if (success) System.out.println("Successfully solved puzzle " + puzzleNumber + " in " + timeTaken + " ms");
        else System.out.println("Failed to solve puzzle " + puzzleNumber + " in " + timeTaken + " ms");
    }

    private static void run(int iterations, int progressUpdateInterval, boolean showAllPuzzles) {
        long startTime = System.currentTimeMillis();
        List<Integer> unsolved = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger completed = new AtomicInteger(0);
        Map<Integer, Long> solveTimes = new ConcurrentHashMap<>();
        ThreadLocal<Sudoku> threadLocalSudoku = ThreadLocal.withInitial(() -> new Sudoku(false));
        ByteBuffer buffer = ByteBuffer.allocate(164 * iterations);
        try (FileChannel fileChannel = new FileInputStream("sudoku.csv").getChannel()) {
            fileChannel.read(buffer);
            buffer.flip();
        } catch (IOException e) {
            System.err.println("Failed to load puzzles file");
            System.exit(1);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> summarizeProgress(completed.get(), showAllPuzzles, startTime, unsolved, solveTimes)));
        IntStream.range(1, iterations).parallel().forEach(i -> {
            Sudoku sudoku = threadLocalSudoku.get();
            byte[] bytes = new byte[Sudoku.BYTES_PER_LINE];
            buffer.get((i - 1) * Sudoku.BYTES_PER_LINE, bytes, 0, Sudoku.BYTES_PER_LINE);
            sudoku.loadPuzzle(new String(bytes));
            long start = System.currentTimeMillis();
            sudoku.solvePuzzle();
            solveTimes.put(i, System.currentTimeMillis() - start);
            if (!sudoku.isSolved()) unsolved.add(i);
            printProgressIfNeeded(iterations, progressUpdateInterval, completed.incrementAndGet());
        });
        summarizeProgress(iterations, showAllPuzzles, startTime, unsolved, solveTimes);
    }

    private static void summarizeProgress(int iterations, boolean showAllPuzzles, long startTime, List<Integer> unsolved, Map<Integer, Long> solveTimes) {
        if (shutdownTriggered) return;
        shutdownTriggered = true;
        double totalTime = (System.currentTimeMillis() - startTime) / 1000.0;
        int solvedPuzzles = iterations - unsolved.size();
        double percent = (double) solvedPuzzles / iterations * 100;
        String percentFormat = percent > 99.99 && percent < 100 ? "%.3f" : "%.2f";
        System.out.printf("Solved %d of %d puzzles (" + percentFormat + "%%) in %.2f seconds%n", solvedPuzzles, iterations, percent, totalTime);
        if (!unsolved.isEmpty()) {
            Collections.sort(unsolved);
            StringBuilder sb = new StringBuilder();
            sb.append("Unsolved puzzles: ");
            int limit = showAllPuzzles ? unsolved.size() : 10;
            sb.append(unsolved.stream().limit(limit).map(String::valueOf).collect(Collectors.joining(", ")));
            if (unsolved.size() > limit) sb.append(", and ").append(unsolved.size() - limit).append(" more...");
            System.out.println(sb);
        }
        if (iterations <= 5000000) {
            String slowest = new HashMap<>(solveTimes).entrySet().stream()
                    .parallel()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(3L)
                    .map(e -> getEntryString(e, unsolved))
                    .collect(Collectors.joining(", "));
            System.out.println("Slowest puzzles: " + slowest);
        }
    }

    private static String getEntryString(Map.Entry<Integer, Long> entry, List<Integer> unsolved) {
        StringBuilder sb = new StringBuilder();
        sb.append(entry.getKey());
        if (unsolved.contains(entry.getKey())) sb.append("*");
        sb.append(" (").append(entry.getValue()).append(" ms)");
        return sb.toString();
    }

    private static void printProgressIfNeeded(int iterations, int progressUpdateInterval, int completedPuzzles) {
        if (progressUpdateInterval > 0 && completedPuzzles % progressUpdateInterval == 0) {
            double percent = (double) completedPuzzles / iterations * 100;
            System.out.printf("Progress: %d / %d (%.2f%%)%n", completedPuzzles, iterations, percent);
        }
    }

}
