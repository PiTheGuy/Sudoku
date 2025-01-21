package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.*;

import java.util.ArrayList;
import java.util.List;

public class HiddenTripletsStrategy extends ByGroupSolveStrategy {
    public HiddenTripletsStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    protected boolean solveGroup(List<Square> squares) {
        boolean changed = false;
        for (int d1 = 1; d1 <= 9; d1++) {
            if (SolverUtils.hasDigitSolved(squares, d1)) continue;
            for (int d2 = d1 + 1; d2 <= 9; d2++) {
                if (SolverUtils.hasDigitSolved(squares, d2)) continue;
                for (int d3 = d2 + 1; d3 <= 9; d3++) {
                    if (SolverUtils.hasDigitSolved(squares, d3)) continue;
                    changed |= process(squares, d1, d2, d3);
                }
            }
        }
        return changed;
    }

    private static boolean process(List<Square> squares, int d1, int d2, int d3) {
        boolean changed = false;
        List<Square> tripletSquares = new ArrayList<>();
        for (Square square : squares) {
            if (square.isSolved()) continue;
            DigitCandidates candidates = square.getCandidates();
            if (candidates.contains(d1) || candidates.contains(d2) || candidates.contains(d3))
                tripletSquares.add(square);
        }
        if (tripletSquares.size() != 3) return false;
        for (Square square : squares) {
            if (tripletSquares.contains(square)) continue;
            if (square.isSolved()) continue;
            DigitCandidates candidates = square.getCandidates();
            if (candidates.contains(d1) || candidates.contains(d2) || candidates.contains(d3)) return false;
        }
        for (Square square : tripletSquares) {
            DigitCandidates candidates = square.getCandidates();
            List<Integer> candidatesList = candidates.getAllCandidates();
            for (int candidate : candidatesList)
                if (candidate != d1 && candidate != d2 && candidate != d3) changed |= candidates.remove(candidate);
        }
        return changed;
    }
}
