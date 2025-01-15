package pitheguy.sudoku.solver;

import org.apache.commons.cli.*;
import pitheguy.sudoku.gui.Sudoku;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SolverChecker {

    public static void main(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(createOptions(), args);
        int iterations = Integer.parseInt(commandLine.getOptionValue("iterations", "10000"));
        int progressUpdateInterval = Integer.parseInt(commandLine.getOptionValue("progressUpdateInterval", "10000"));
        int maxShownPuzzles = Integer.parseInt(commandLine.getOptionValue("maxShownPuzzles", "10"));
        run(iterations, progressUpdateInterval, maxShownPuzzles);
    }

    public static Options createOptions() {
        Options options = new Options();
        options.addOption("iterations", true, "Number of iterations to run");
        options.addOption("progressUpdateInterval", true, "Progress update interval");
        options.addOption("maxShownPuzzles", true, "Maximum number of unsolved puzzles to show on completion");
        return options;
    }

    private static void run(int iterations, int progressUpdateInterval, int maxShownPuzzles) {
        List<Integer> unsolved = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger completed = new AtomicInteger(0);
        ThreadLocal<Sudoku> threadLocalSudoku = ThreadLocal.withInitial(() -> new Sudoku(false));
        IntStream.range(1, iterations).parallel().forEach(i -> {
            Sudoku sudoku = threadLocalSudoku.get();
            sudoku.loadPuzzle(i);
            sudoku.solvePuzzle();
            if (!sudoku.isSolved()) unsolved.add(i);
            printProgressIfNeeded(iterations, progressUpdateInterval, completed.incrementAndGet());
        });

        int solvedPuzzles = iterations - unsolved.size();
        double percent = (double) solvedPuzzles / iterations * 100;
        System.out.printf("Solved %d of %d puzzles (%.2f%%)%n", solvedPuzzles, iterations, percent);
        if (!unsolved.isEmpty()) {
            Collections.sort(unsolved);
            StringBuilder sb = new StringBuilder();
            sb.append("Unsolved puzzles: ");
            sb.append(unsolved.stream().limit(maxShownPuzzles).map(String::valueOf).collect(Collectors.joining(", ")));
            if (unsolved.size() > maxShownPuzzles) sb.append(", and ").append(unsolved.size() - maxShownPuzzles).append(" more...");
            System.out.println(sb);
        }
    }

    private static void printProgressIfNeeded(int iterations, int progressUpdateInterval, int completedPuzzles) {
        if (progressUpdateInterval == 0) return;
        if (completedPuzzles % progressUpdateInterval == 0) System.out.println("Progress: " + completedPuzzles + " / " + iterations);
    }


}
