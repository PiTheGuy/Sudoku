package pitheguy.sudoku.solver;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.strategies.*;

import java.util.Arrays;
import java.util.List;

public class SudokuSolver {
    private static final boolean DEBUG = false;
    private static final boolean RUN_ALL = false;
    private final Sudoku sudoku;

    public SudokuSolver(Sudoku sudoku) {
        this.sudoku = sudoku;
    }

    public void solve() {
        setupCandidates(sudoku);
        while (!sudoku.isSolved()) {
            boolean changed = stepSolve();
            if (!changed || DEBUG) break;
        }
    }

    private boolean stepSolve() {
        List<SolveStrategy> strategies = Arrays.asList(
                new NakedSinglesStrategy(),
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
                new XYChainsStrategy(),
                new JellyfishStrategy(),
                new UniqueRectanglesStrategy(),
                new HiddenUniqueRectanglesStrategy(),
                new AlignedPairExclusionStrategy(),
                new AlmostLockedSetStrategy(),
                new PatternOverlayMethodStrategy()
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

    private static void setupCandidates(Sudoku sudoku) {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                Square square = sudoku.getSquare(row, col);
                if (square.isSolved()) continue;
                DigitCandidates candidates = square.getCandidates();
                removeInvalidCandidates(candidates, square.getSurroundingRow());
                removeInvalidCandidates(candidates, square.getSurroundingColumn());
                removeInvalidCandidates(candidates, square.getSurroundingBox());
            }
        }
    }

    private static void removeInvalidCandidates(DigitCandidates candidates, List<Square> squares) {
        for (Square square : squares) {
            if (!square.isSolved()) continue;
            int digit = square.getValue().charAt(0) - '0';
            candidates.remove(digit);
        }
    }
}
