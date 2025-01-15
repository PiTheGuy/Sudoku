package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SolveStrategy;

import java.util.List;

public class CandidateRemovalStrategy implements SolveStrategy {
    @Override
    public boolean solve(Sudoku sudoku) {
        boolean changed = false;
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                Square square = sudoku.getSquare(row, col);
                if (square.isSolved()) continue;
                DigitCandidates candidates = square.getCandidates();
                changed |= removeInvalidCandidates(candidates, square.getSurroundingRow());
                changed |= removeInvalidCandidates(candidates, square.getSurroundingColumn());
                changed |= removeInvalidCandidates(candidates, square.getSurroundingBox());
                if (candidates.count() == 1) {
                    square.setValue(String.valueOf(candidates.getFirst()));
                    changed = true;
                }
            }
        }
        return changed;
    }



    private static boolean removeInvalidCandidates(DigitCandidates candidates, List<Square> squares) {
        boolean changed = false;
        for (Square square : squares) {
            if (!square.isSolved()) continue;
            int digit = square.getValue().charAt(0) - '0';
            changed |= candidates.remove(digit);
        }
        return changed;
    }
}
