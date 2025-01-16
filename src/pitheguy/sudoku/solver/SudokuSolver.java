package pitheguy.sudoku.solver;

import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.strategies.*;

import java.util.Arrays;
import java.util.List;

public class SudokuSolver {
    private static final boolean DEBUG = false;
    private static final boolean RUN_ALL = true;
    private final Sudoku sudoku;

    public SudokuSolver(Sudoku sudoku) {
        this.sudoku = sudoku;
    }

    public void solve() {
        while (!sudoku.isSolved()) {
            boolean changed = stepSolve();
            if (!changed || DEBUG) break;
        }
    }

    private boolean stepSolve() {
        List<SolveStrategy> strategies = Arrays.asList(
                new CandidateRemovalStrategy(),
                new HiddenSinglesStrategy(),
                new PointingPairsAndTriosStrategy(),
                new ClaimingPairsAndTriosStrategy(),
                new NakedPairsStrategy(),
                new HiddenPairsStrategy(),
                new NakedTripletsStrategy(),
                new HiddenTripletsStrategy(),
                new XWingStrategy(),
                new XYWingStrategy(),
                new BugStrategy(),
                new RectangleEliminationStrategy(),
                new SwordfishStrategy(),
                new XYZWingStrategy(),
                new UniqueRectanglesStrategy(),
                new XYChainsStrategy(),
                new JellyfishStrategy()
        );

        boolean anySolved = false;

        for (SolveStrategy strategy : strategies) {
            if (strategy.solve(sudoku)) {
                if (!RUN_ALL) return true;
                anySolved = true;
            }
        }

        return anySolved;
    }
}
