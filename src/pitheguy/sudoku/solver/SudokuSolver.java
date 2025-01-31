package pitheguy.sudoku.solver;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.strategies.*;

import java.util.Arrays;
import java.util.List;

public class SudokuSolver {
    public static final boolean DEBUG = false;
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
                new NakedSinglesStrategy(sudoku),
                new HiddenSinglesStrategy(sudoku),
                new PointingPairsAndTriosStrategy(sudoku),
                new ClaimingPairsAndTriosStrategy(sudoku),
                new NakedPairsStrategy(sudoku),
                new HiddenPairsStrategy(sudoku),
                new NakedTripletsStrategy(sudoku),
                new HiddenTripletsStrategy(sudoku),
                new XWingStrategy(sudoku),
                new ChuteRemotePairsStrategy(sudoku),
                new XYWingStrategy(sudoku),
                new BugStrategy(sudoku),
                new RectangleEliminationStrategy(sudoku),
                new SwordfishStrategy(sudoku),
                new XYZWingStrategy(sudoku),
                new XCyclesStrategy(sudoku),
                new XYChainsStrategy(sudoku),
                new JellyfishStrategy(sudoku),
                new UniqueRectanglesStrategy(sudoku),
                new HiddenUniqueRectanglesStrategy(sudoku),
                new WXYZWingStrategy(sudoku),
                new AlignedPairExclusionStrategy(sudoku),
                new AlmostLockedSetStrategy(sudoku),
                new AlternatingInferenceChainsStrategy(sudoku),
                new PatternOverlayMethodStrategy(sudoku)
        );

        boolean anySolved = false;

        for (SolveStrategy strategy : strategies) {
            if (strategy.solve()) {
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
