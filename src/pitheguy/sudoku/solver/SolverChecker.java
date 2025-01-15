package pitheguy.sudoku.solver;

import pitheguy.sudoku.gui.Sudoku;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SolverChecker {
    public static final int ITERATIONS = 25000;
    public static final int PROGRESS_UPDATE_INTERVAL = 10000;
    public static final int MAX_SHOWN_PUZZLES = 10;

    public static void main(String[] args) {
        List<Integer> unsolved = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger completed = new AtomicInteger(0);
        ThreadLocal<Sudoku> threadLocalSudoku = ThreadLocal.withInitial(() -> new Sudoku(false));
        IntStream.range(1, ITERATIONS).parallel().forEach(i -> {
            Sudoku sudoku = threadLocalSudoku.get();
            sudoku.loadPuzzle(i);
            sudoku.solvePuzzle();
            if (!sudoku.isSolved()) unsolved.add(i);
            if (completed.incrementAndGet() % PROGRESS_UPDATE_INTERVAL == 0) System.out.println("Progress: " + completed.get() + " / " + ITERATIONS);
        });

        int solvedPuzzles = ITERATIONS - unsolved.size();
        double percent = (double) solvedPuzzles / ITERATIONS * 100;
        System.out.printf("Solved %d of %d puzzles (%.2f%%)%n", solvedPuzzles, ITERATIONS, percent);
        if (!unsolved.isEmpty()) {
            Collections.sort(unsolved);
            StringBuilder sb = new StringBuilder();
            sb.append("Unsolved puzzles: ");
            sb.append(unsolved.stream().limit(MAX_SHOWN_PUZZLES).map(String::valueOf).collect(Collectors.joining(", ")));
            if (unsolved.size() > MAX_SHOWN_PUZZLES) sb.append(", and ").append(unsolved.size() - MAX_SHOWN_PUZZLES).append(" more...");
            System.out.println(sb);
        }
    }


}
