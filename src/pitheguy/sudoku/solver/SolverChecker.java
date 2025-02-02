package pitheguy.sudoku.solver;

import org.apache.commons.cli.*;
import pitheguy.sudoku.gui.Sudoku;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
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
        PuzzleInfo puzzleInfo = parsePuzzleInfo(commandLine);
        int progressUpdateInterval = Integer.parseInt(commandLine.getOptionValue("progressUpdateInterval", "50000"));
        boolean showAllPuzzles = commandLine.hasOption("all");
        Optional<File> unsolvedOutput = Optional.ofNullable(commandLine.getOptionValue("unsolvedOutput")).map(File::new);
        run(puzzleInfo, progressUpdateInterval, showAllPuzzles, unsolvedOutput);
    }

    private static PuzzleInfo parsePuzzleInfo(CommandLine commandLine) {
        if (commandLine.hasOption("puzzleIndexes")) {
            IntStream.Builder builder = IntStream.builder();
            int size = 0;
            int max = 0;
            try (Scanner scanner = new Scanner(new File(commandLine.getOptionValue("puzzleIndexes")))) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    int puzzleIndex = Integer.parseInt(line);
                    builder.add(puzzleIndex);
                    if (puzzleIndex > max) max = puzzleIndex;
                    size++;
                }
            } catch (IOException e) {
                System.err.println("Failed to read puzzle index file.");
                System.exit(1);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse puzzle index file.");
                System.exit(1);
            }
            return new PuzzleInfo(builder.build(), size, max);
        } else {
            int iterations = Integer.parseInt(commandLine.getOptionValue("iterations", "10000"));
            return new PuzzleInfo(IntStream.range(1, iterations), iterations, iterations);
        }
    }

    public static Options createOptions() {
        Options options = new Options();
        options.addOption("singlePuzzle", true, "Check a single puzzle");
        options.addOption("puzzleIndexes", true, "Load puzzle indexes to test from a file");
        options.addOption("iterations", true, "Number of iterations to run");
        options.addOption("progressUpdateInterval", true, "Progress update interval");
        options.addOption("all", "Show all unsolved puzzles");
        options.addOption("unsolvedOutput", true, "Output unsolved puzzles to file. Implies -all");
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

    private static void run(PuzzleInfo puzzleInfo, int progressUpdateInterval, boolean showAllPuzzles, Optional<File> unsolvedOutput) {
        long startTime = System.currentTimeMillis();
        List<Integer> unsolved = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger completed = new AtomicInteger(0);
        Map<Integer, Long> solveTimes = new ConcurrentHashMap<>();
        ThreadLocal<Sudoku> threadLocalSudoku = ThreadLocal.withInitial(() -> new Sudoku(false));
        ByteBuffer buffer = ByteBuffer.allocate(164 * puzzleInfo.max());
        try (FileChannel fileChannel = new FileInputStream("sudoku.csv").getChannel()) {
            fileChannel.read(buffer);
            buffer.flip();
        } catch (IOException e) {
            System.err.println("Failed to load puzzles file");
            System.exit(1);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> summarizeProgress(completed.get(), showAllPuzzles, startTime, unsolved, solveTimes, unsolvedOutput)));
        puzzleInfo.puzzles().parallel().forEach(i -> {
            Sudoku sudoku = threadLocalSudoku.get();
            byte[] bytes = new byte[Sudoku.BYTES_PER_LINE];
            buffer.get((i - 1) * Sudoku.BYTES_PER_LINE, bytes, 0, Sudoku.BYTES_PER_LINE);
            sudoku.loadPuzzle(new String(bytes));
            long start = System.currentTimeMillis();
            sudoku.solvePuzzle();
            solveTimes.put(i, System.currentTimeMillis() - start);
            if (!sudoku.isSolved()) unsolved.add(i);
            printProgressIfNeeded(puzzleInfo.size(), progressUpdateInterval, completed.incrementAndGet());
        });
        summarizeProgress(puzzleInfo.size(), showAllPuzzles, startTime, unsolved, solveTimes, unsolvedOutput);
    }

    private static void summarizeProgress(int iterations, boolean showAllPuzzles, long startTime, List<Integer> unsolved, Map<Integer, Long> solveTimes, Optional<File> unsolvedOutput) {
        if (shutdownTriggered) return;
        shutdownTriggered = true;
        double totalTime = (System.currentTimeMillis() - startTime) / 1000.0;
        int solvedPuzzles = iterations - unsolved.size();
        double percent = (double) solvedPuzzles / iterations * 100;
        if (percent > 99.999 && percent < 100) percent = 99.999; // Don't show 100% unless all puzzles are solved
        String percentFormat = percent > 99.99 && percent < 100 ? "%.3f" : "%.2f";
        System.out.printf("Solved %d of %d puzzles (" + percentFormat + "%%) in %.2f seconds%n", solvedPuzzles, iterations, percent, totalTime);
        if (!unsolved.isEmpty()) {
            Collections.sort(unsolved);
            if (unsolvedOutput.isPresent()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(unsolvedOutput.get()))) {
                    for (Integer puzzle : unsolved) writer.write(puzzle + "\n");
                    System.out.println("Unsolved puzzles written to " + unsolvedOutput.get().getAbsolutePath());
                } catch (IOException e) {
                    System.out.println("Failed to write unsolved puzzles to file");
                }
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Unsolved puzzles: ");
                int limit = showAllPuzzles ? unsolved.size() : 10;
                sb.append(unsolved.stream().limit(limit).map(String::valueOf).collect(Collectors.joining(", ")));
                if (unsolved.size() > limit) sb.append(", and ").append(unsolved.size() - limit).append(" more...");
                System.out.println(sb);
            }
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

    private record PuzzleInfo(IntStream puzzles, int size, int max) {}

}
