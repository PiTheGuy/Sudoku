package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;

import java.util.*;

public class BugStrategy implements SolveStrategy {
    @Override
    public boolean solve(Sudoku sudoku) {
        boolean changed = false;
        Optional<Square> bugSquareOptional = SolverUtils.getOnlySquareThat(sudoku.getAllSquares(),
                square -> square.getCandidates().count() != 2,
                true);
        if (bugSquareOptional.isEmpty()) return false;
        Square bugSquare = bugSquareOptional.get();
        for (int digit : bugSquare.getCandidates().getAllCandidates()) {
            if (wouldCreateDeadlyCondition(bugSquare.getSurroundingRow(), digit) &&
                wouldCreateDeadlyCondition(bugSquare.getSurroundingColumn(), digit) &&
                wouldCreateDeadlyCondition(bugSquare.getSurroundingBox(), digit)) {
                bugSquare.setValue(String.valueOf(digit));
                changed = true;
            }
        }
        return changed;
    }

    private boolean wouldCreateDeadlyCondition(List<Square> squares, int removedDigit) {
        Map<Integer, Integer> candidateCounts = new HashMap<>();
        for (Square square : squares) {
            if (square.isSolved()) continue;
            List<Integer> candidates = square.getCandidates().getAllCandidates();
            for (int candidate : candidates)
                candidateCounts.put(candidate, candidateCounts.getOrDefault(candidate, 0) + 1);
        }
        candidateCounts.put(removedDigit, candidateCounts.get(removedDigit) - 1);
        for (int digit = 1; digit <= 9; digit++) if (candidateCounts.getOrDefault(digit, 0) != 2) return false;
        return true;
    }
}
